<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import ConfirmationModal from '$lib/components/ConfirmationModal.svelte';
    import { fetchApi, fetchJson } from '$lib/api';
    import { auth, getAvatarUrl } from '$lib/stores/auth.svelte';
    import { toast } from '$lib/stores/toast.svelte';

    let customNickname = $state('');
    let isSaving = $state(false);
    let isSyncing = $state(false);

    let deleteModal: ReturnType<typeof ConfirmationModal>;

    onMount(() => {
        const params = new URLSearchParams(window.location.search);
        if (params.get('sync') === 'success') {
            toast.success('Profile successfully synced with Discord!');
            window.history.replaceState({}, '', window.location.pathname);
        }

        if (auth.user) {
            customNickname = auth.user.nickname || '';
        }
    });

    async function updateNickname() {
        if (!auth.user || customNickname === (auth.user.nickname || '')) return;

        isSaving = true;
        try {
            await fetchApi('/api/users/me/nickname', {
                method: 'POST',
                body: JSON.stringify({ nickname: customNickname })
            });
            await auth.refreshUser();
            toast.success('Nickname updated successfully!');
        } catch {
            toast.error('Failed to update nickname.');
        } finally {
            isSaving = false;
        }
    }

    async function syncDiscord() {
        isSyncing = true;
        try {
            const data = await fetchJson<{ url: string }>('/api/auth/discord/url');
            sessionStorage.setItem('ark_sync_redirect', 'true');
            window.location.href = data.url;
        } catch {
            toast.error('Failed to initiate sync with Discord.');
            isSyncing = false;
        }
    }

    async function confirmDelete() {
        try {
            await fetchApi('/api/users/me/delete', { method: 'POST' });
            toast.success('Account deleted successfully. We hope to see you again!');
            auth.logout();
        } catch {
            toast.error('Failed to delete account.');
        }
    }
</script>

<svelte:head>
    <title>Profile - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-2xl">
        <div class="flex items-center gap-4 mb-8">
            <a href="{base}/dashboard" class="btn btn-ghost btn-circle" aria-label="Back to Dashboard">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </a>
            <h1 class="text-3xl font-bold text-ubuntu">User Profile</h1>
        </div>

        {#if auth.user}
            <div class="space-y-6">
                <div class="card bg-base-100 shadow-xl overflow-hidden">
                    <div class="h-32 bg-gradient-to-r from-primary via-accent to-secondary"></div>
                    <div class="card-body -mt-16 items-center text-center">
                        <div class="avatar ring-offset-base-100 ring-offset-4 ring-primary ring-4 rounded-full">
                            <div class="w-32 rounded-full bg-neutral shadow-2xl overflow-hidden">
                                <img src={getAvatarUrl(auth.user.discordId, auth.user.avatar)} alt={auth.user.username} />
                            </div>
                        </div>
                        <div class="mt-4">
                            <h2 class="text-3xl font-bold text-ubuntu">{customNickname || auth.user.username}</h2>
                            <p class="opacity-60 flex items-center justify-center gap-2 font-mono text-sm">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 127.14 96.36"><path d="M107.7,8.07A105.15,105.15,0,0,0,81.47,0a72.06,72.06,0,0,0-3.36,6.83A97.68,97.68,0,0,0,49,6.83,72.37,72.37,0,0,0,45.64,0,105.89,105.89,0,0,0,19.39,8.09C2.79,32.65-1.71,56.6.54,80.21h0A105.73,105.73,0,0,0,32.71,96.36,77.7,77.7,0,0,0,39.6,85.25a68.42,68.42,0,0,1-10.85-5.18c.91-.66,1.8-1.34,2.66-2a75.57,75.57,0,0,0,64.32,0c.87.71,1.76,1.39,2.66,2a68.68,68.68,0,0,1-10.87,5.19,77,77,0,0,0,6.89,11.1A105.25,105.25,0,0,0,126.6,80.22h0C129.24,52.84,122.09,29.11,107.7,8.07ZM42.45,65.69C36.18,65.69,31,60,31,53s5-12.74,11.43-12.74S54,46,53.89,53C53.89,60,48.84,65.69,42.45,65.69Zm42.24,0C78.41,65.69,73.31,60,73.31,53s5-12.74,11.43-12.74S96.16,46,96.06,53C96.06,60,91.08,65.69,84.69,65.69Z"/></svg>
                                {auth.user.username}
                            </p>
                        </div>
                    </div>
                </div>

                <div class="card bg-base-100 shadow-xl">
                    <div class="card-body">
                        <h3 class="text-xl font-bold mb-4 text-ubuntu">Account Settings</h3>

                        <div class="w-full">
                            <label for="nickname" class="block text-sm font-bold text-ubuntu mb-2">Custom Nickname</label>
                            <div class="flex flex-col sm:flex-row gap-2">
                                <input
                                    id="nickname"
                                    type="text"
                                    placeholder={auth.user.username}
                                    class="input input-bordered flex-1 font-neo focus:ring-primary"
                                    bind:value={customNickname}
                                    onkeydown={(e) => e.key === 'Enter' && updateNickname()}
                                />
                                <button
                                    class="btn btn-primary"
                                    disabled={isSaving || customNickname === (auth.user.nickname || '')}
                                    onclick={updateNickname}
                                >
                                    {#if isSaving}
                                        <span class="loading loading-spinner"></span>
                                    {/if}
                                    Save Changes
                                </button>
                            </div>
                            <p class="text-xs opacity-60 mt-2 font-neo italic">This changes your display name in Ark Resolver. Leave empty to use Discord username.</p>
                        </div>

                        <div class="divider"></div>

                        <div class="flex justify-between items-center mb-4">
                            <h3 class="text-xl font-bold text-ubuntu">Discord Information</h3>
                            <button
                                class="btn btn-ghost btn-sm gap-2"
                                onclick={syncDiscord}
                                disabled={isSyncing}
                            >
                                {#if isSyncing}
                                    <span class="loading loading-spinner loading-xs"></span>
                                {:else}
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" /></svg>
                                {/if}
                                Sync with Discord
                            </button>
                        </div>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                            <div class="bg-base-200 p-3 rounded-lg">
                                <span class="block opacity-60 uppercase text-xs font-bold mb-1">Discord Username</span>
                                <span class="font-mono">{auth.user.username}</span>
                            </div>
                            <div class="bg-base-200 p-3 rounded-lg">
                                <span class="block opacity-60 uppercase text-xs font-bold mb-1">Discord User ID</span>
                                <span class="font-mono">{auth.user.discordId}</span>
                            </div>
                        </div>

                        <div class="divider"></div>

                        <div class="bg-error/10 border border-error/20 p-6 rounded-lg mt-4">
                            <h4 class="text-error font-bold text-lg mb-2 text-ubuntu">Danger Zone</h4>
                            <p class="text-sm opacity-80 mb-4 font-neo">Deleting your account will remove all your data and dissociate your Discord account from Ark Resolver.</p>
                            <button class="btn btn-error btn-outline w-full sm:w-auto" onclick={() => deleteModal.show()}>
                                Delete My Account
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        {/if}
    </div>
</main>

<ConfirmationModal
    bind:this={deleteModal}
    id="delete-account-modal"
    title="Delete Account"
    message="Are you absolutely sure you want to delete your account? This action is irreversible and all your data will be permanently removed."
    confirmText="Delete My Account"
    type="error"
    onConfirm={confirmDelete}
/>
