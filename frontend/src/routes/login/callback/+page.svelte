<script lang="ts">
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import { fetchJson } from '$lib/api';

	let status = $state('Authenticating with Discord...');
	let error = $state('');

	onMount(async () => {
		const urlParams = new URLSearchParams(window.location.search);
		const code = urlParams.get('code');

		if (!code) {
			error = 'No authentication code provided by Discord.';
			return;
		}

		try {
			const data = await fetchJson<{ token: string }>('/api/auth/discord', {
				method: 'POST',
				body: JSON.stringify({ code })
			});
			
			// Store the JWT token securely
			localStorage.setItem('ark_token', data.token);
			
            const isSync = sessionStorage.getItem('ark_sync_redirect') === 'true';
            sessionStorage.removeItem('ark_sync_redirect');

			status = isSync ? 'Sync successful! Returning to profile...' : 'Success! Redirecting to dashboard...';
			
			// Short delay for UX
			setTimeout(() => {
				window.location.href = isSync ? `${base}/profile?sync=success` : `${base}/dashboard?login=success`;
			}, 1000);

		} catch (err) {
			console.error(err);
			error = 'Failed to exchange authentication code. Please try again.';
		}
	});
</script>

<svelte:head>
	<title>Authenticating - Ark Resolver</title>
</svelte:head>

<main class="min-h-screen bg-base-200 flex flex-col justify-center items-center p-4">
	<div class="card w-full max-w-md bg-base-100 shadow-xl border-t-4 border-primary">
		<div class="card-body items-center text-center">
			
			{#if error}
				<div class="text-error mb-4">
					<svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
					<h2 class="card-title justify-center text-xl">Login Failed</h2>
				</div>
				<p>{error}</p>
				<div class="card-actions mt-6 w-full">
					<a href="{base}/login" class="btn btn-primary w-full">Try Again</a>
				</div>
			{:else}
				<span class="loading loading-spinner loading-lg text-primary mb-4"></span>
				<h2 class="card-title text-xl">{status}</h2>
				<p class="text-base-content/70 text-sm mt-2">Please wait while we securely log you in.</p>
			{/if}

		</div>
	</div>
</main>
