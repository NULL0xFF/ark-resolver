export interface UserResponse {
    id: string;
    discordId: string;
    username: string;
    nickname: string | null;
    avatar: string | null;
}

export type GroupRole = 'MANAGER' | 'AUDITOR' | 'MEMBER';

export interface GroupResponse {
    id: string;
    name: string;
    description: string | null;
    maxPartiesPerCharacter: number;
}

export interface GroupMemberResponse {
    id: string;
    username: string;
    role: GroupRole;
    discordId: string;
    avatar: string | null;
}

export type ScheduleStatus = 'ACTIVE' | 'PLANNED' | 'PAST';

export interface ScheduleResponse {
    id: string;
    groupId: string;
    title: string;
    start: string;
    end: string;
    status: ScheduleStatus;
}

export type PartyStatus = 'Planned' | 'On-going' | 'Done';

export interface PartyMemberResponse {
    characterId: string;
    characterName: string;
    characterClass: string | null;
    itemLevel: number | null;
    userId: string;
}

export interface PartyResponse {
    id: string;
    scheduleId: string;
    title: string;
    raidType: string;
    members: number;
    max: number;
    status: PartyStatus;
    start: string;
    joinedMembers: PartyMemberResponse[];
}

export interface CharacterResponse {
    id: string;
    userId: string;
    name: string;
    characterClass: string | null;
    itemLevel: number | null;
}

export interface InviteCodeResponse {
    code: string;
    max: number | null;
    used: number;
    expires: string;
}

export interface AvailabilityBlock {
    start: string;
    end: string;
}

export interface AvailabilityResponse {
    userId: string;
    blocks: AvailabilityBlock[];
}
