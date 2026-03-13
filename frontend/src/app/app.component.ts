import { DatePipe } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { MessageResponse } from './core/models/message.model';
import { RoomResponse, RoomUserResponse } from './core/models/room.model';
import { ChatWebSocketService } from './core/services/chat-websocket.service';
import { MessageService } from './core/services/message.service';
import { RoomService } from './core/services/room.service';
import { UserService } from './core/services/user.service';

type ScreenStep = 'nickname' | 'chat';
type RoomActionMode = 'none' | 'create' | 'search';

interface ChatMessage {
  id: string;
  author: string;
  body: string;
  createdAt: Date;
  mine: boolean;
}

interface ChatRoom {
  id: string;
  code: string;
  name: string;
  ownerId: string;
  messages: ChatMessage[];
  members: RoomUserResponse[];
  usersById: Record<string, string>;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule, DatePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnDestroy {
  @ViewChild('messageList') private messageList?: ElementRef<HTMLElement>;
  private readonly userService = inject(UserService);
  private readonly roomService = inject(RoomService);
  private readonly messageService = inject(MessageService);
  private readonly chatWebSocketService = inject(ChatWebSocketService);

  private currentUserId: string | null = null;
  private readonly roomRealtimeUnsubscribers = new Map<string, () => void>();
  private readonly roomPresenceUnsubscribers = new Map<string, () => void>();
  private readonly presenceJoinedRoomIds = new Set<string>();
  private disconnectTriggered = false;

  protected screenStep: ScreenStep = 'nickname';
  protected roomActionMode: RoomActionMode = 'none';

  protected nicknameInput = '';
  protected nickname = '';
  protected nicknameError = '';

  protected createRoomNameInput = '';
  protected searchRoomCodeInput = '';

  protected roomError = '';
  protected roomInfo = '';

  protected joinedRooms: ChatRoom[] = [];
  protected activeRoomCode: string | null = null;
  protected showMembersPanel = false;

  protected draft = '';

  protected get activeRoom(): ChatRoom | null {
    return this.joinedRooms.find((room) => room.code === this.activeRoomCode) ?? null;
  }

  protected async enterChat(): Promise<void> {
    const trimmedNickname = this.nicknameInput.trim();
    if (trimmedNickname.length < 2) {
      this.nicknameError = 'Please enter a nickname with at least 2 characters.';
      return;
    }

    try {
      const user = await firstValueFrom(this.userService.create({ nickname: trimmedNickname }));
      this.currentUserId = user.id;
      this.nickname = user.nickname;
      this.screenStep = 'chat';
      this.nicknameError = '';
      void this.chatWebSocketService.connect();
    } catch (error) {
      this.nicknameError = this.getErrorMessage(error, 'Unable to register user.');
    }
  }

  protected showCreateRoom(): void {
    this.roomActionMode = 'create';
    this.roomError = '';
    this.roomInfo = '';
  }

  protected showSearchRoom(): void {
    this.roomActionMode = 'search';
    this.roomError = '';
    this.roomInfo = '';
  }

  protected async createRoom(): Promise<void> {
    if (!this.currentUserId) {
      this.roomError = 'Please enter chat before creating a room.';
      return;
    }

    const roomName = this.createRoomNameInput.trim();

    if (roomName.length < 2) {
      this.roomError = 'Provide room name (2+) to create a room.';
      return;
    }

    try {
      const room = await firstValueFrom(
        this.roomService.create({
          name: roomName,
          ownerId: this.currentUserId
        })
      );

      await this.openRoom(room);
      this.createRoomNameInput = '';
      this.roomActionMode = 'none';
      this.roomError = '';
      this.roomInfo = `Room #${room.code} created and opened.`;
    } catch (error) {
      this.roomError = this.getErrorMessage(error, 'Unable to create room.');
    }
  }

  protected async searchRoom(): Promise<void> {
    if (!this.currentUserId) {
      this.roomError = 'Please enter chat before opening a room.';
      return;
    }

    const roomCode = this.searchRoomCodeInput.trim().toUpperCase();

    if (roomCode.length < 3) {
      this.roomError = 'Enter a valid room code.';
      return;
    }

    try {
      const room = await firstValueFrom(
        this.roomService.joinByCode({
          code: roomCode,
          userId: this.currentUserId
        })
      );

      await this.openRoom(room);
      this.searchRoomCodeInput = '';
      this.roomActionMode = 'none';
      this.roomError = '';
      this.roomInfo = `Room #${room.code} opened.`;
    } catch (error) {
      this.roomError = this.getErrorMessage(error, `Room with code ${roomCode} was not found.`);
    }
  }

  protected selectRoom(code: string): void {
    const previousRoomCode = this.activeRoomCode;
    this.activeRoomCode = code;
    this.showMembersPanel = false;
    void this.syncPresenceForActiveRoom(previousRoomCode, code);
    this.scrollToBottom();
  }

  protected async leaveRoom(code: string, event?: Event): Promise<void> {
    event?.stopPropagation();

    if (!this.currentUserId) {
      return;
    }

    const room = this.joinedRooms.find((joinedRoom) => joinedRoom.code === code);
    if (!room) {
      return;
    }

    try {
      await firstValueFrom(
        this.roomService.leave(room.id, {
          userId: this.currentUserId
        })
      );
    } catch (error) {
      this.roomError = this.getErrorMessage(error, `Unable to leave room #${room.code}.`);
      return;
    }

    const previousActiveCode = this.activeRoomCode;
    const remainingRooms = this.joinedRooms.filter((joinedRoom) => joinedRoom.code !== code);
    const nextActiveCode = previousActiveCode === code ? (remainingRooms[0]?.code ?? null) : previousActiveCode;

    await this.detachRoomRealtime(room.id);
    await this.syncPresenceForActiveRoom(previousActiveCode, nextActiveCode);

    this.joinedRooms = remainingRooms;
    this.activeRoomCode = nextActiveCode;
    if (previousActiveCode === code || !nextActiveCode) {
      this.showMembersPanel = false;
    }

    this.roomError = '';
    this.roomInfo = `You left room #${room.code}.`;
  }

  protected toggleMembersPanel(): void {
    this.showMembersPanel = !this.showMembersPanel;
  }

  protected async sendMessage(): Promise<void> {
    const room = this.activeRoom;
    const messageBody = this.draft.trim();

    if (!room || !messageBody || !this.currentUserId) {
      return;
    }

    try {
      await this.chatWebSocketService.sendRoomMessage(room.id, {
        userId: this.currentUserId,
        content: messageBody
      });
      this.draft = '';
      this.roomError = '';
      this.scrollToBottom();
    } catch (error) {
      try {
        const response = await firstValueFrom(
          this.messageService.send(room.id, {
            userId: this.currentUserId,
            content: messageBody
          })
        );
        this.handleIncomingMessage(room.id, response);
        this.draft = '';
        this.roomError = '';
      } catch (fallbackError) {
        this.roomError = this.getErrorMessage(fallbackError, this.getErrorMessage(error, 'Unable to send message.'));
      }
    }
  }

  protected handleComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void this.sendMessage();
    }
  }

  private async openRoom(room: RoomResponse): Promise<void> {
    if (!this.currentUserId) {
      return;
    }

    const [roomUsers, recentMessages] = await Promise.all([
      firstValueFrom(this.roomService.listUsers(room.id)),
      firstValueFrom(this.messageService.listRecent(room.id, this.currentUserId, 100))
    ]);

    const usersById: Record<string, string> = Object.fromEntries(
      roomUsers.map((roomUser) => [roomUser.id, roomUser.nickname])
    );
    usersById[this.currentUserId] = this.nickname;

    const messages = recentMessages
      .map((message) => this.toChatMessage(message, usersById))
      .sort((left, right) => left.createdAt.getTime() - right.createdAt.getTime());

    const joinedRoom: ChatRoom = {
      id: room.id,
      code: room.code,
      name: room.name,
      ownerId: room.ownerId,
      members: this.sortMembers(roomUsers),
      usersById,
      messages
    };

    this.upsertRoom(joinedRoom);

    try {
      await this.ensureRealtimeSubscription(room.id);
      await this.ensurePresenceRealtimeSubscription(room.id);
    } catch {
      this.roomInfo = `Room #${room.code} opened without realtime updates.`;
    }
    const previousRoomCode = this.activeRoomCode;
    this.activeRoomCode = joinedRoom.code;
    this.showMembersPanel = false;
    void this.syncPresenceForActiveRoom(previousRoomCode, joinedRoom.code);
    this.scrollToBottom();
  }

  private async ensureRealtimeSubscription(roomId: string): Promise<void> {
    if (this.roomRealtimeUnsubscribers.has(roomId)) {
      return;
    }

    const unsubscribe = await this.chatWebSocketService.subscribeRoomMessages(roomId, (message) => {
      this.handleIncomingMessage(roomId, message);
    });

    this.roomRealtimeUnsubscribers.set(roomId, unsubscribe);
  }

  private async ensurePresenceRealtimeSubscription(roomId: string): Promise<void> {
    if (this.roomPresenceUnsubscribers.has(roomId)) {
      return;
    }

    const unsubscribe = await this.chatWebSocketService.subscribeRoomPresence(roomId, (payload) => {
      this.handlePresenceUpdate(roomId, payload);
    });

    this.roomPresenceUnsubscribers.set(roomId, unsubscribe);
  }

  private async syncPresenceForActiveRoom(
    previousRoomCode: string | null,
    nextRoomCode: string | null
  ): Promise<void> {
    if (!this.currentUserId) {
      return;
    }

    const previousRoomId = this.joinedRooms.find((room) => room.code === previousRoomCode)?.id ?? null;
    const nextRoomId = this.joinedRooms.find((room) => room.code === nextRoomCode)?.id ?? null;

    if (previousRoomId && previousRoomId !== nextRoomId && this.presenceJoinedRoomIds.has(previousRoomId)) {
      try {
        await this.chatWebSocketService.leaveRoomPresence(previousRoomId, this.currentUserId);
      } catch {
        // Keep UI responsive even if leave presence fails.
      } finally {
        this.presenceJoinedRoomIds.delete(previousRoomId);
      }
    }

    if (nextRoomId && !this.presenceJoinedRoomIds.has(nextRoomId)) {
      try {
        await this.chatWebSocketService.joinRoomPresence(nextRoomId, this.currentUserId);
        this.presenceJoinedRoomIds.add(nextRoomId);
      } catch {
        // Presence join can fail independently from chat loading.
      }
    }
  }

  private async detachRoomRealtime(roomId: string): Promise<void> {
    const messageUnsubscribe = this.roomRealtimeUnsubscribers.get(roomId);
    if (messageUnsubscribe) {
      messageUnsubscribe();
      this.roomRealtimeUnsubscribers.delete(roomId);
    }

    const presenceUnsubscribe = this.roomPresenceUnsubscribers.get(roomId);
    if (presenceUnsubscribe) {
      presenceUnsubscribe();
      this.roomPresenceUnsubscribers.delete(roomId);
    }

    if (this.currentUserId && this.presenceJoinedRoomIds.has(roomId)) {
      try {
        await this.chatWebSocketService.leaveRoomPresence(roomId, this.currentUserId);
      } catch {
        // Ignore disconnect failures during room cleanup.
      } finally {
        this.presenceJoinedRoomIds.delete(roomId);
      }
    }
  }

  private handlePresenceUpdate(roomId: string, payload: unknown): void {
    const room = this.joinedRooms.find((joinedRoom) => joinedRoom.id === roomId);
    if (!room) {
      return;
    }

    const presenceMembers = this.extractPresenceMembers(payload, room.ownerId);
    if (!presenceMembers) {
      return;
    }

    const sortedMembers = this.sortMembers(presenceMembers);
    const usersById = { ...room.usersById };
    for (const member of sortedMembers) {
      usersById[member.id] = member.nickname;
    }

    const updatedRoom: ChatRoom = {
      ...room,
      members: sortedMembers,
      usersById
    };

    this.upsertRoom(updatedRoom);
  }

  private handleIncomingMessage(roomId: string, message: MessageResponse): void {
    const room = this.joinedRooms.find((joinedRoom) => joinedRoom.id === roomId);
    if (!room) {
      return;
    }

    if (room.messages.some((existingMessage) => existingMessage.id === message.id)) {
      return;
    }

    const usersById = {
      ...room.usersById
    };
    if (this.currentUserId) {
      usersById[this.currentUserId] = this.nickname;
    }
    if (message.nickname?.trim()) {
      usersById[message.userId] = message.nickname;
    }

    const members = this.upsertMember(room.members, message, room.ownerId);

    const updatedRoom: ChatRoom = {
      ...room,
      members,
      usersById,
      messages: [...room.messages, this.toChatMessage(message, usersById)].sort(
        (left, right) => left.createdAt.getTime() - right.createdAt.getTime()
      )
    };

    this.upsertRoom(updatedRoom);
    this.scrollToBottom();
  }

  private toChatMessage(message: MessageResponse, usersById: Record<string, string>): ChatMessage {
    const mine = message.userId === this.currentUserId;
    const parsedDate = new Date(message.sentAt);
    const createdAt = Number.isNaN(parsedDate.getTime()) ? new Date() : parsedDate;

    return {
      id: message.id,
      author: mine ? this.nickname : (message.nickname ?? usersById[message.userId] ?? message.userId),
      body: message.content,
      createdAt,
      mine
    };
  }

  private upsertMember(
    members: RoomUserResponse[],
    message: MessageResponse,
    ownerId: string
  ): RoomUserResponse[] {
    const nickname = message.nickname?.trim();
    if (!nickname) {
      return members;
    }

    const existingMemberIndex = members.findIndex((member) => member.id === message.userId);
    if (existingMemberIndex === -1) {
      return this.sortMembers([
        ...members,
        { id: message.userId, nickname, role: message.userId === ownerId ? 'owner' : 'member' }
      ]);
    }

    const existingMember = members[existingMemberIndex];
    if (existingMember.nickname === nickname) {
      return members;
    }

    const updatedMembers = members.map((member, index) =>
      index === existingMemberIndex ? { ...member, nickname } : member
    );
    return this.sortMembers(updatedMembers);
  }

  private extractPresenceMembers(payload: unknown, ownerId: string): RoomUserResponse[] | null {
    if (Array.isArray(payload)) {
      return this.normalizePresenceMembers(payload, ownerId);
    }

    if (!this.isRecord(payload)) {
      return null;
    }

    const candidates = [payload['members'], payload['users'], payload['onlineUsers'], payload['payload']];
    for (const candidate of candidates) {
      if (!Array.isArray(candidate)) {
        continue;
      }

      const normalized = this.normalizePresenceMembers(candidate, ownerId);
      if (normalized.length > 0) {
        return normalized;
      }
    }

    return null;
  }

  private normalizePresenceMembers(items: unknown[], ownerId: string): RoomUserResponse[] {
    const members: RoomUserResponse[] = [];

    for (const item of items) {
      const normalized = this.normalizePresenceMember(item, ownerId);
      if (normalized) {
        members.push(normalized);
      }
    }

    return members;
  }

  private normalizePresenceMember(item: unknown, ownerId: string): RoomUserResponse | null {
    if (!this.isRecord(item)) {
      return null;
    }

    const id = typeof item['id'] === 'string'
      ? item['id']
      : (typeof item['userId'] === 'string' ? item['userId'] : null);
    const nickname = typeof item['nickname'] === 'string' ? item['nickname'] : null;
    if (!id || !nickname) {
      return null;
    }

    const role = item['role'] === 'owner' || id === ownerId ? 'owner' : 'member';
    return { id, nickname, role };
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }

  private sortMembers(members: RoomUserResponse[]): RoomUserResponse[] {
    return [...members].sort((left, right) => {
      if (left.role === 'owner' && right.role !== 'owner') {
        return -1;
      }
      if (left.role !== 'owner' && right.role === 'owner') {
        return 1;
      }

      return left.nickname.localeCompare(right.nickname);
    });
  }

  private upsertRoom(room: ChatRoom): void {
    const index = this.joinedRooms.findIndex((joinedRoom) => joinedRoom.code === room.code);
    if (index === -1) {
      this.joinedRooms = [...this.joinedRooms, room];
      return;
    }

    this.joinedRooms = this.joinedRooms.map((joinedRoom, joinedRoomIndex) =>
      joinedRoomIndex === index ? room : joinedRoom
    );
  }

  private getErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof Error && error.message.trim()) {
      return error.message;
    }

    return fallback;
  }

  @HostListener('window:beforeunload')
  @HostListener('window:pagehide')
  protected handleWindowUnload(): void {
    this.triggerUserDisconnect();
  }

  ngOnDestroy(): void {
    this.triggerUserDisconnect();

    if (this.currentUserId) {
      for (const roomId of this.presenceJoinedRoomIds) {
        void this.chatWebSocketService.leaveRoomPresence(roomId, this.currentUserId);
      }
    }

    for (const unsubscribe of this.roomRealtimeUnsubscribers.values()) {
      unsubscribe();
    }
    this.roomRealtimeUnsubscribers.clear();

    for (const unsubscribe of this.roomPresenceUnsubscribers.values()) {
      unsubscribe();
    }
    this.roomPresenceUnsubscribers.clear();
    this.presenceJoinedRoomIds.clear();
  }

  private triggerUserDisconnect(): void {
    if (this.disconnectTriggered || !this.currentUserId) {
      return;
    }

    this.disconnectTriggered = true;
    this.userService.disconnectOnUnload(this.currentUserId);
  }

  private scrollToBottom(): void {
    queueMicrotask(() => {
      const listElement = this.messageList?.nativeElement;
      if (!listElement) {
        return;
      }

      listElement.scrollTop = listElement.scrollHeight;
    });
  }
}
