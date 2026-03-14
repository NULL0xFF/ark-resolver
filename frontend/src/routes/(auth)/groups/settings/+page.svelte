<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import { page } from '$app/stores';
    import ConfirmationModal from '$lib/components/ConfirmationModal.svelte';
    import { fetchApi, fetchJson, ApiError } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import { toast } from '$lib/stores/toast.svelte';
    import type { GroupResponse, GroupMemberResponse } from '$lib/types/api';

    let groupId = $derived($page.url.searchParams.get('id') ?? '');
    let groupName = $state('');
    let description = $state('');
    let maxPartiesPerCharacter = $state(3);
    let isLoading = $state(true);
    let isSaving = $state(false);
    let error = $state('');
    let members = $state<GroupMemberResponse[]>([]);

    let deleteModal: ReturnType<typeof ConfirmationModal>;
    let leaveModal: ReturnType<typeof ConfirmationModal>;
    let transferModal: ReturnType<typeof ConfirmationModal>;
    let selectedMemberId = $state('');

    let isManager = $derived(members.find(m => m.id === auth.user?.id)?.role === 'MANAGER');
    let otherMembers = $derived(members.filter(m => m.id !== auth.user?.id));

    onMount(async () => {
        try {
            const [group, memberList] = await Promise.all([
                fetchJson<GroupResponse>(`/api/groups/detail?groupId=${groupId}`),
                fetchJson<GroupMemberResponse[]>(`/api/members?groupId=${groupId}`)
            ]);
            groupName = group.name;
            description = group.description || '';
            maxPartiesPerCharacter = group.maxPartiesPerCharacter ?? 3;
            members = memberList;
        } catch (err) {
            if (!(err instanceof ApiError && err.status === 401)) {
                error = 'Failed to load group settings.';
            }
        } finally {
            isLoading = false;
        }
    });

    async function handleSubmit() {
        if (!groupName.trim() || !isManager) return;

        isSaving = true;
        try {
            await fetchApi(`/api/groups/update`, {
                method: 'POST',
                body: JSON.stringify({ groupId, name: groupName, description, maxPartiesPerCharacter })
            });
            toast.success('Group settings updated!');
            window.location.href = `${base}/groups/detail?id=${groupId}`;
        } catch {
            toast.error('Failed to update group settings.');
        } finally {
            isSaving = false;
        }
    }

    async function onConfirmDelete() {
        try {
            await fetchApi(`/api/groups/delete`, { method: 'POST', body: JSON.stringify({ groupId: groupId }) });
            toast.success('Group deleted.');
            window.location.href = `${base}/dashboard`;
        } catch {
            toast.error('Failed to delete group.');
        }
    }

    async function onConfirmLeave() {
        if (!auth.user) return;
        try {
            await fetchApi(`/api/members/remove`, { method: 'POST', body: JSON.stringify({ groupId: groupId, targetUserId: auth.user.id }) });
            toast.success('You have left the group.');
            window.location.href = `${base}/dashboard`;
        } catch {
            toast.error('Failed to leave group.');
        }
    }

    async function onConfirmTransfer() {
        if (!selectedMemberId) return;
        try {
            await fetchApi(`/api/members/transfer`, { method: 'POST', body: JSON.stringify({ groupId: groupId, targetUserId: selectedMemberId }) });
            toast.success('Management transferred successfully.');
            window.location.href = `${base}/groups/detail?id=${groupId}`;
        } catch {
            toast.error('Failed to transfer management.');
        }
    }
</script>

<svelte:head>
    <title>Group Settings - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-2xl">
        <div class="flex items-center gap-4 mb-8">
            <a href="{base}/groups/detail?id={groupId}" class="btn btn-ghost btn-circle" aria-label="Back to Group">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </a>
            <h1 class="text-3xl font-bold text-ubuntu">Group Settings</h1>
        </div>

        {#if isLoading}
            <div class="flex justify-center items-center h-64">
                <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>
        {:else if error}
            <div class="alert alert-error shadow-lg">
                <span>{error}</span>
                <a href="{base}/groups/detail?id={groupId}" class="btn btn-sm btn-ghost">Return</a>
            </div>
        {:else}
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="space-y-6">
                        <div>
                            <label for="group-name" class="block text-sm font-bold text-ubuntu mb-2">Group Name</label>
                            {#if isManager}
                                <input
                                    id="group-name"
                                    type="text"
                                    class="input input-bordered w-full font-neo focus:ring-primary"
                                    bind:value={groupName}
                                    onkeydown={(e) => e.key === 'Enter' && handleSubmit()}
                                />
                            {:else}
                                <p class="text-xl font-bold font-ubuntu px-1">{groupName}</p>
                            {/if}
                        </div>

                        <div>
                            <label for="max-parties" class="block text-sm font-bold text-ubuntu mb-2">Party Slots per Character per Schedule</label>
                            {#if isManager}
                                <input
                                    id="max-parties"
                                    type="number"
                                    class="input input-bordered w-full font-neo focus:ring-primary"
                                    bind:value={maxPartiesPerCharacter}
                                    min="1"
                                    max="10"
                                />
                                <p class="text-xs opacity-50 mt-1">How many parties a character can join within a single schedule (default: 3).</p>
                            {:else}
                                <p class="text-xl font-bold font-ubuntu px-1">{maxPartiesPerCharacter}</p>
                            {/if}
                        </div>

                        <div>
                            <label for="description" class="block text-sm font-bold text-ubuntu mb-2">Description</label>
                            {#if isManager}
                                <textarea
                                    id="description"
                                    class="textarea textarea-bordered h-32 w-full font-neo focus:ring-primary"
                                    bind:value={description}
                                ></textarea>
                            {:else}
                                <p class="text-base font-neo opacity-80 px-1 whitespace-pre-wrap">{description || 'No description provided.'}</p>
                            {/if}
                        </div>
                    </div>

                    {#if isManager}
                        <div class="card-actions justify-end mt-8">
                            <button
                                class="btn btn-primary px-8"
                                disabled={isSaving || !groupName.trim()}
                                onclick={handleSubmit}
                            >
                                {#if isSaving}
                                    <span class="loading loading-spinner"></span>
                                {/if}
                                Save Changes
                            </button>
                        </div>
                    {/if}
                </div>
            </div>

            {#if isManager}
                <div class="card bg-base-100 shadow-xl mt-8 border-t-4 border-info">
                    <div class="card-body">
                        <h3 class="font-bold text-lg mb-2 text-ubuntu">Transfer Management</h3>
                        <p class="text-sm opacity-80 mb-4 font-neo">Transfer the MANAGER role to another member. You will become a regular MEMBER and lose administrative access.</p>

                        <div class="flex flex-col sm:flex-row gap-2">
                            <select class="select select-bordered flex-1" bind:value={selectedMemberId}>
                                <option value="" disabled selected>Select a member to promote</option>
                                {#each otherMembers as member}
                                    <option value={member.id}>{member.username}</option>
                                {/each}
                            </select>
                            <button
                                class="btn btn-info"
                                disabled={!selectedMemberId}
                                onclick={() => transferModal.show()}
                            >
                                Transfer Role
                            </button>
                        </div>
                        {#if otherMembers.length === 0}
                            <p class="text-xs text-info mt-2 italic">No other members available to transfer role to.</p>
                        {/if}
                    </div>
                </div>
            {/if}

            <div class="card bg-base-100 shadow-xl mt-8 border-t-4 border-error">
                <div class="card-body">
                    <h3 class="text-error font-bold text-lg mb-2 text-ubuntu">Danger Zone</h3>
                    {#if isManager}
                        <p class="text-sm opacity-80 mb-4 font-neo">Deleting this group will remove all schedules, parties, and memberships permanently.</p>
                        <button class="btn btn-error btn-outline w-full sm:w-auto" onclick={() => deleteModal.show()}>Delete This Group</button>
                    {:else}
                        <p class="text-sm opacity-80 mb-4 font-neo">Leaving this group will remove your access to its schedules and parties.</p>
                        <button class="btn btn-error btn-outline w-full sm:w-auto" onclick={() => leaveModal.show()}>Leave This Group</button>
                    {/if}
                </div>
            </div>
        {/if}
    </div>
</main>

<ConfirmationModal
    bind:this={deleteModal}
    id="delete-group-modal"
    title="Delete Group"
    message="Are you absolutely sure you want to delete this group? All schedules, parties, and data will be permanently removed for all members."
    confirmText="Delete Permanently"
    type="error"
    onConfirm={onConfirmDelete}
/>

<ConfirmationModal
    bind:this={leaveModal}
    id="leave-group-modal"
    title="Leave Group"
    message="Are you sure you want to leave this group? You will lose access to all its schedules and your availability data will be removed."
    confirmText="Leave Group"
    type="warning"
    onConfirm={onConfirmLeave}
/>

<ConfirmationModal
    bind:this={transferModal}
    id="transfer-manager-modal"
    title="Transfer Management"
    message="Are you sure you want to transfer group management? You will immediately lose your manager status and become a regular member."
    confirmText="Transfer and Demote"
    type="warning"
    onConfirm={onConfirmTransfer}
/>
