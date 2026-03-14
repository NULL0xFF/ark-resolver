<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import ThemeToggle from '$lib/components/ThemeToggle.svelte';
    import { auth, getAvatarUrl } from '$lib/stores/auth.svelte';

    let { children } = $props();

    onMount(async () => {
        await auth.initialize();
        if (!auth.isAuthenticated) {
            window.location.href = `${base}/login`;
        }
    });
</script>

{#if auth.isLoading}
    <div class="min-h-screen bg-base-200 flex items-center justify-center">
        <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>
{:else if auth.isAuthenticated}
    <div class="min-h-screen bg-base-200 flex flex-col">
        <div class="navbar bg-base-100 shadow-sm sticky top-0 z-50">
            <div class="navbar-start">
                <a class="btn btn-ghost text-xl font-bold" href="{base}/dashboard">Ark Resolver</a>
            </div>
            <div class="navbar-end gap-2">
                <ThemeToggle />
                {#if auth.user}
                    <div class="dropdown dropdown-end">
                        <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                        <!-- svelte-ignore a11y_label_has_associated_control -->
                        <label tabindex="0" class="btn btn-ghost btn-circle avatar">
                            <div class="w-9 rounded-full ring-2 ring-primary ring-offset-base-100 ring-offset-1">
                                <img src={getAvatarUrl(auth.user.discordId, auth.user.avatar)} alt={auth.user.username} />
                            </div>
                        </label>
                        <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                        <ul tabindex="0" class="dropdown-content z-[100] menu p-2 shadow-xl bg-base-100 rounded-box w-52 border border-base-200 mt-2">
                            <li class="menu-title px-4 py-2">
                                <span class="font-bold text-sm">{auth.user.nickname || auth.user.username}</span>
                                <span class="text-xs opacity-50 font-mono">@{auth.user.username}</span>
                            </li>
                            <div class="divider my-0"></div>
                            <li><a href="{base}/profile">Edit Profile</a></li>
                            <li><a href="{base}/dashboard">Dashboard</a></li>
                            <div class="divider my-0"></div>
                            <li><button class="text-error" onclick={() => auth.logout()}>Logout</button></li>
                        </ul>
                    </div>
                {/if}
            </div>
        </div>

        {@render children()}
    </div>
{/if}
