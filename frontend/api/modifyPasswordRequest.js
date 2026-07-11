import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

export const changePassword = async password => {
    const result = requestJson(`${getServerUrl()}/v1/users/me/password`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
            password,
        }),
    });
    return result;
};
