import { base } from '$app/paths';
import { fetchJson } from '$lib/api';
import type { UserResponse } from '$lib/types/api';

let user = $state<UserResponse | null>(null);
let isLoading = $state(true);
let isAuthenticated = $state(false);
let initialized = false;

export const auth = {
    get user() { return user; },
    get isLoading() { return isLoading; },
    get isAuthenticated() { return isAuthenticated; },

    async initialize() {
        if (initialized) return;
        initialized = true;
        const token = localStorage.getItem('ark_token');
        if (!token) {
            isLoading = false;
            return;
        }
        try {
            user = await fetchJson<UserResponse>('/api/users/me');
            isAuthenticated = true;
        } catch {
            localStorage.removeItem('ark_token');
        } finally {
            isLoading = false;
        }
    },

    async refreshUser() {
        try {
            user = await fetchJson<UserResponse>('/api/users/me');
        } catch {
            // fetchApi handles 401 by clearing the token and redirecting
        }
    },

    logout(redirectPath?: string) {
        localStorage.removeItem('ark_token');
        user = null;
        isAuthenticated = false;
        initialized = false;
        isLoading = true;
        window.location.href = redirectPath ?? `${base}/`;
    }
};

export function getAvatarUrl(discordId?: string | null, avatarHash?: string | null): string {
    if (!discordId) return 'https://cdn.discordapp.com/embed/avatars/0.png';
    if (!avatarHash) return `https://cdn.discordapp.com/embed/avatars/${parseInt(discordId) % 5}.png`;
    return `https://cdn.discordapp.com/avatars/${discordId}/${avatarHash}.png`;
}
