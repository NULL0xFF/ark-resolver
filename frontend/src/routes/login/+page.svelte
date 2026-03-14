<script lang="ts">
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import ThemeToggle from '$lib/components/ThemeToggle.svelte';
	import { fetchJson } from '$lib/api';
    import { toast } from '$lib/stores/toast.svelte';

	let isLoading = $state(false);

	onMount(() => {
		const params = new URLSearchParams(window.location.search);
		const error = params.get('error');
		
		if (error === 'session_invalid') {
			toast.warning('Your session has been invalidated. Please login again.');
			// Clean up URL
			window.history.replaceState({}, '', window.location.pathname);
		}

		if (localStorage.getItem('ark_token')) {
			window.location.href = `${base}/dashboard`;
		}
	});

	async function loginWithDiscord() {
		try {
			isLoading = true;
			const data = await fetchJson<{ url: string }>('/api/auth/discord/url');
			window.location.href = data.url;
		} catch (error) {
			console.error('Login error:', error);
			isLoading = false;
			toast.error('Failed to connect to the authentication server.');
		}
	}
</script>

<svelte:head>
	<title>Login - Ark Resolver</title>
</svelte:head>

<main class="min-h-screen bg-base-200 flex flex-col justify-center items-center relative p-4">
	<div class="absolute top-4 right-4 flex items-center gap-2">
		<a href="{base}/" class="btn btn-ghost">Home</a>
		<ThemeToggle />
	</div>

	<div class="card w-full max-w-md bg-base-100 shadow-xl border-t-4 border-primary">
		<div class="card-body items-center text-center">
			<h2 class="card-title text-3xl font-bold mb-2 font-ubuntu">Welcome Back</h2>
			<p class="text-base-content/70 mb-8 font-neo">Login or register to access your groups.</p>
			
			<button class="btn btn-primary btn-lg w-full gap-3 text-lg" disabled={isLoading} onclick={loginWithDiscord} aria-label="Login with Discord">
				{#if isLoading}
					<span class="loading loading-spinner"></span>
				{:else}
					<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="currentColor" viewBox="0 0 127.14 96.36">
						<path d="M107.7,8.07A105.15,105.15,0,0,0,81.47,0a72.06,72.06,0,0,0-3.36,6.83A97.68,97.68,0,0,0,49,6.83,72.37,72.37,0,0,0,45.64,0,105.89,105.89,0,0,0,19.39,8.09C2.79,32.65-1.71,56.6.54,80.21h0A105.73,105.73,0,0,0,32.71,96.36,77.7,77.7,0,0,0,39.6,85.25a68.42,68.42,0,0,1-10.85-5.18c.91-.66,1.8-1.34,2.66-2a75.57,75.57,0,0,0,64.32,0c.87.71,1.76,1.39,2.66,2a68.68,68.68,0,0,1-10.87,5.19,77,77,0,0,0,6.89,11.1A105.25,105.25,0,0,0,126.6,80.22h0C129.24,52.84,122.09,29.11,107.7,8.07ZM42.45,65.69C36.18,65.69,31,60,31,53s5-12.74,11.43-12.74S54,46,53.89,53C53.89,60,48.84,65.69,42.45,65.69Zm42.24,0C78.41,65.69,73.31,60,73.31,53s5-12.74,11.43-12.74S96.16,46,96.06,53C96.06,60,91.08,65.69,84.69,65.69Z"/>
					</svg>
				{/if}
				Login with Discord
			</button>

            <div class="divider mt-8">Or</div>
            
            <a href="{base}/" class="btn btn-outline w-full">Return Home</a>
		</div>
	</div>
</main>
