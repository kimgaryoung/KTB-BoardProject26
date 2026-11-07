"""Deploy a digest-pinned image through SSM without an inbound SSH connection."""
import base64
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import time

DELIVERY_TIMEOUT = 120
EXECUTION_TIMEOUT = 600
POLL_TIMEOUT = DELIVERY_TIMEOUT + EXECUTION_TIMEOUT + 60


def request(instance, component, image, region, registry, port, script=None):
    if not re.fullmatch(r'i-(?:[a-f0-9]{8}|[a-f0-9]{17})', instance):
        raise ValueError('Set a valid EC2 instance ID')
    if component not in ('backend', 'frontend', 'intro'):
        raise ValueError('Unknown deployment component')
    if not re.fullmatch(r'[a-z0-9./_-]+@sha256:[a-f0-9]{64}', image):
        raise ValueError('Deployments require an immutable image digest')
    if not re.fullmatch(r'[a-z0-9-]+', region) or not re.fullmatch(r'[a-z0-9.-]+', registry):
        raise ValueError('Invalid region or registry')
    if not image.startswith(registry + '/') or not port.isdigit() or not 0 < int(port) <= 65535:
        raise ValueError('Image registry or deployment port does not match')
    source = script or Path(__file__).with_name('deploy-remote.sh')
    encoded = base64.b64encode(source.read_bytes()).decode('ascii')
    args = shlex.join([component, image, region, registry, port])
    command = f"printf '%s' {shlex.quote(encoded)} | base64 --decode | bash -s -- {args}"
    return {
        'InstanceIds': [instance],
        'DocumentName': 'AWS-RunShellScript',
        'Comment': f'Board {component} immutable image deployment',
        'TimeoutSeconds': DELIVERY_TIMEOUT,
        'Parameters': {'commands': ['set -eu', command], 'executionTimeout': [str(EXECUTION_TIMEOUT)]},
    }


def aws(args):
    return json.loads(subprocess.check_output(
        args + ['--region', os.environ['AWS_REGION']], text=True, stderr=subprocess.PIPE))


def send(payload, run=aws):
    with tempfile.TemporaryDirectory(prefix='board-ssm-') as directory:
        path = Path(directory) / 'request.json'
        path.write_text(json.dumps(payload))
        result = run(['aws', 'ssm', 'send-command', '--cli-input-json', f'file://{path}', '--output', 'json'])
    return result['Command']['CommandId']


def invocation(command_id, instance, run=aws):
    try:
        return run(['aws', 'ssm', 'get-command-invocation', '--command-id', command_id,
                    '--instance-id', instance, '--output', 'json'])
    except subprocess.CalledProcessError as error:
        if 'InvocationDoesNotExist' in (error.stderr or ''):
            return None
        raise


def wait_for_command(fetch, timeout=POLL_TIMEOUT, sleep=time.sleep, now=time.monotonic):
    start = now()
    while now() - start < timeout:
        result = fetch()
        if result is None or result.get('Status') in ('Pending', 'InProgress', 'Delayed', 'Cancelling'):
            sleep(5)
            continue
        if result.get('Status') == 'Success' and result.get('ResponseCode') == 0:
            return result
        details = result.get('StandardErrorContent') or result.get('StandardOutputContent') or ''
        raise RuntimeError(f"SSM deployment {result.get('Status')}: {details[-4000:]}")
    raise TimeoutError('SSM command polling deadline exceeded; check the recorded command ID before retrying')


def main():
    release = json.loads(Path('release/release.json').read_text())
    instance = os.environ['DEPLOY_INSTANCE_ID']
    component = os.environ['COMPONENT']
    image = release['image_ref']
    payload = request(instance, component, image, os.environ['AWS_REGION'],
                      image.split('/')[0], os.environ['DEPLOY_PORT'])
    command_id = send(payload)
    Path('ssm-command.json').write_text(json.dumps({
        'command_id': command_id, 'instance_id': instance, 'component': component, 'image_ref': image,
    }, indent=2))
    print(f'SSM command {command_id} on {instance}', flush=True)
    result = wait_for_command(lambda: invocation(command_id, instance))
    print(result.get('StandardOutputContent', '')[-4000:])


if __name__ == '__main__':
    main()
