"""기능 : 독립 인트로 정적 빌드 / 설명 : 첫 화면 자산을 /intro 네임스페이스로 분리해 서비스 이미지 없이 교체할 수 있게 합니다."""
import shutil
import sys
from pathlib import Path


def build(source, output):
    output.mkdir(parents=True, exist_ok=True)
    for path in source.rglob('*'):
        if not path.is_file():
            continue
        relative = path.relative_to(source)
        target = output / 'intro' / relative
        if relative.as_posix() == 'html/index.html':
            target = output / 'index.html'
        target.parent.mkdir(parents=True, exist_ok=True)
        if path.suffix in ('.html', '.css', '.js'):
            text = path.read_text()
            for prefix in ('/css/', '/js/', '/public/'):
                text = text.replace(prefix, '/intro' + prefix)
            target.write_text(text)
        else:
            shutil.copyfile(path, target)


if __name__ == '__main__':
    build(Path(sys.argv[1]), Path(sys.argv[2]))
