<script lang="ts">
    import { base } from '$app/paths';
    import { fetchApi, ApiError } from '$lib/api';
    import { toast } from '$lib/stores/toast.svelte';

    let groupName = $state('');
    let description = $state('');
    let isSubmitting = $state(false);

    async function handleSubmit() {
        if (!groupName.trim()) return;

        isSubmitting = true;
        try {
            await fetchApi('/api/groups', {
                method: 'POST',
                body: JSON.stringify({ name: groupName, description })
            });
            window.location.href = `${base}/dashboard?group_created=success`;
        } catch (err) {
            if (!(err instanceof ApiError && err.status === 401)) {
                toast.error('An error occurred while creating the group.');
            }
        } finally {
            isSubmitting = false;
        }
    }
</script>

<svelte:head>
    <title>Create Group - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-2xl">
        <div class="flex items-center gap-4 mb-8">
            <a href="{base}/dashboard" class="btn btn-ghost btn-circle" aria-label="Back to Dashboard">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </a>
            <h1 class="text-3xl font-bold text-ubuntu">Create New Group</h1>
        </div>

        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <div class="space-y-6">
                    <div>
                        <label for="group-name" class="block text-sm font-bold text-ubuntu mb-2">Group Name</label>
                        <input
                            id="group-name"
                            type="text"
                            placeholder="e.g., Friday Night Static"
                            class="input input-bordered w-full font-neo focus:ring-primary"
                            bind:value={groupName}
                            onkeydown={(e) => e.key === 'Enter' && handleSubmit()}
                        />
                    </div>

                    <div>
                        <label for="description" class="block text-sm font-bold text-ubuntu mb-2">Description (Optional)</label>
                        <textarea
                            id="description"
                            class="textarea textarea-bordered h-32 w-full font-neo focus:ring-primary"
                            placeholder="Describe your group's goals or schedule..."
                            bind:value={description}
                        ></textarea>
                    </div>
                </div>

                <div class="card-actions justify-end mt-8">
                    <a href="{base}/dashboard" class="btn btn-ghost">Cancel</a>
                    <button
                        class="btn btn-primary px-8"
                        disabled={isSubmitting || !groupName.trim()}
                        onclick={handleSubmit}
                    >
                        {#if isSubmitting}
                            <span class="loading loading-spinner"></span>
                        {/if}
                        Create Group
                    </button>
                </div>
            </div>
        </div>
    </div>
</main>
