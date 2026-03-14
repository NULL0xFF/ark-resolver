<script lang="ts">
    import { base } from '$app/paths';
    import { fetchApi, ApiError } from '$lib/api';
    import { toast } from '$lib/stores/toast.svelte';

    let inviteCode = $state('');
    let isSubmitting = $state(false);

    async function handleSubmit() {
        if (!inviteCode.trim()) return;

        isSubmitting = true;
        try {
            await fetchApi('/api/groups/join', {
                method: 'POST',
                body: JSON.stringify({ code: inviteCode.trim() })
            });
            window.location.href = `${base}/dashboard?group_joined=success`;
        } catch (err) {
            if (err instanceof ApiError && err.status === 400) {
                toast.error('Invalid or expired invite code.');
            } else if (!(err instanceof ApiError && err.status === 401)) {
                toast.error('An error occurred while joining the group.');
            }
        } finally {
            isSubmitting = false;
        }
    }
</script>

<svelte:head>
    <title>Join Group - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-xl">
        <div class="flex items-center gap-4 mb-8">
            <a href="{base}/dashboard" class="btn btn-ghost btn-circle" aria-label="Back to Dashboard">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </a>
            <h1 class="text-3xl font-bold text-ubuntu">Join a Group</h1>
        </div>

        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <div class="space-y-6">
                    <div>
                        <label for="invite-code" class="block text-sm font-bold text-ubuntu mb-2">Invite Code</label>
                        <input
                            id="invite-code"
                            type="text"
                            placeholder="e.g., ARK-X78F-QZ21"
                            class="input input-bordered w-full font-mono text-center tracking-widest text-lg focus:ring-primary"
                            bind:value={inviteCode}
                            onkeydown={(e) => e.key === 'Enter' && handleSubmit()}
                        />
                    </div>
                    <p class="text-sm opacity-70 text-center">Ask your group manager for an invite code. Codes are case-sensitive.</p>
                </div>

                <div class="card-actions justify-end mt-8">
                    <a href="{base}/dashboard" class="btn btn-ghost">Cancel</a>
                    <button
                        class="btn btn-secondary px-8"
                        disabled={isSubmitting || !inviteCode.trim()}
                        onclick={handleSubmit}
                    >
                        {#if isSubmitting}
                            <span class="loading loading-spinner"></span>
                        {/if}
                        Join Group
                    </button>
                </div>
            </div>
        </div>
    </div>
</main>
