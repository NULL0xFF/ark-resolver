import { base } from '$app/paths';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export class ApiError extends Error {
    public status: number;
    public data: any;

    constructor(status: number, data: any, message: string) {
        super(message);
        this.status = status;
        this.data = data;
        this.name = 'ApiError';
    }
}

export async function fetchApi(endpoint: string, options: RequestInit = {}) {
    const token = localStorage.getItem('ark_token');
    
    const headers = new Headers(options.headers || {});
    if (token && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    
    if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
        headers.set('Content-Type', 'application/json');
    }

    const config: RequestInit = {
        ...options,
        headers
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

    if (!response.ok) {
        let errorData;
        try {
            errorData = await response.json();
        } catch {
            errorData = { message: 'An unknown error occurred.' };
        }

        if (response.status === 401) {
            localStorage.removeItem('ark_token');
            window.location.href = `${base}/login?error=session_invalid`;
        }

        throw new ApiError(response.status, errorData, errorData.message || response.statusText);
    }

    return response;
}

export async function fetchJson<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const response = await fetchApi(endpoint, options);
    if (response.status === 204) {
        return {} as T;
    }
    return response.json();
}
