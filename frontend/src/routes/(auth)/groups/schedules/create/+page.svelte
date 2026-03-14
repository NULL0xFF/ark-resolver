<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import { page } from '$app/stores';
    import { fetchApi, fetchJson, ApiError } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import { toast } from '$lib/stores/toast.svelte';
    import type { GroupMemberResponse } from '$lib/types/api';

    let groupId = $derived($page.url.searchParams.get('id') ?? '');
    let title = $state('');
    let members = $state<GroupMemberResponse[]>([]);
    let isLoading = $state(true);

    let isAdmin = $derived(
        members.find(m => m.id === auth.user?.id)?.role === 'MANAGER' ||
        members.find(m => m.id === auth.user?.id)?.role === 'AUDITOR'
    );

    const getNowISO = () => {
        const d = new Date();
        d.setMinutes(0, 0, 0);
        return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    };

    const getNextWeekISO = () => {
        const d = new Date();
        d.setDate(d.getDate() + 7);
        d.setMinutes(0, 0, 0);
        return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    };

    let startDate = $state(getNowISO());
    let endDate = $state(getNextWeekISO());
    let isSubmitting = $state(false);

    onMount(async () => {
        try {
            members = await fetchJson<GroupMemberResponse[]>(`/api/members?groupId=${groupId}`);
            if (!isAdmin) {
                toast.error("You don't have permission to create schedules.");
                window.location.href = `${base}/groups/detail?id=${groupId}`;
            }
        } catch (err) {
            console.error(err);
        } finally {
            isLoading = false;
        }
    });

    $effect(() => {
        if (startDate && endDate && new Date(startDate) >= new Date(endDate)) {
            const d = new Date(startDate);
            d.setDate(d.getDate() + 1);
            endDate = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
        }
    });

    async function handleSubmit() {
        if (!title.trim() || !startDate || !endDate) return;

        isSubmitting = true;
        try {
            await fetchApi(`/api/schedules/create`, {
                method: 'POST',
                body: JSON.stringify({
                    groupId,
                    title,
                    startTime: new Date(startDate).toISOString(),
                    endTime: new Date(endDate).toISOString()
                })
            });
            toast.success('Schedule created successfully!');
            window.location.href = `${base}/groups/detail?id=${groupId}`;
        } catch (err) {
            if (!(err instanceof ApiError && err.status === 401)) {
                toast.error('Failed to create schedule.');
            }
        } finally {
            isSubmitting = false;
        }
    }
</script>

<svelte:head>
    <title>Create Schedule - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-2xl">
        <div class="flex items-center gap-4 mb-8">
            <a href="{base}/groups/detail?id={groupId}" class="btn btn-ghost btn-circle" aria-label="Back to Group">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </a>
            <h1 class="text-3xl font-bold text-ubuntu">Create Schedule</h1>
        </div>

        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <div class="space-y-6">
                    <div>
                        <label for="title" class="block text-sm font-bold text-ubuntu mb-2">Schedule Title</label>
                        <input
                            id="title"
                            type="text"
                            placeholder="e.g., March Week 1 Reset"
                            class="input input-bordered w-full font-neo focus:ring-primary"
                            bind:value={title}
                        />
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label for="start-date" class="block text-sm font-bold text-ubuntu mb-2">Start Time</label>
                            <input
                                id="start-date"
                                type="datetime-local"
                                class="input input-bordered w-full font-mono"
                                bind:value={startDate}
                            />
                        </div>
                        <div>
                            <label for="end-date" class="block text-sm font-bold text-ubuntu mb-2">End Time</label>
                            <input
                                id="end-date"
                                type="datetime-local"
                                class="input input-bordered w-full font-mono"
                                bind:value={endDate}
                            />
                        </div>
                    </div>
                </div>

                <div class="card-actions justify-end mt-8">
                    <a href="{base}/groups/detail?id={groupId}" class="btn btn-ghost">Cancel</a>
                    <button
                        class="btn btn-secondary px-8"
                        disabled={isSubmitting || !title.trim() || !startDate || !endDate}
                        onclick={handleSubmit}
                    >
                        {#if isSubmitting}
                            <span class="loading loading-spinner"></span>
                        {/if}
                        Create Schedule
                    </button>
                </div>
            </div>
        </div>
    </div>
</main>
