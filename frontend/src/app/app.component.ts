import { DatePipe } from '@angular/common';
import { Component, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

type ScreenStep = 'nickname' | 'chat';
type RoomActionMode = 'none' | 'create' | 'search';

interface ChatMessage {
  id: number;
  author: string;
  body: string;
  createdAt: Date;
  mine: boolean;
}

interface RoomCatalogEntry {
  code: string;
  name: string;
}

interface ChatRoom {
  code: string;
  name: string;
  messages: ChatMessage[];
  nextMessageId: number;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule, DatePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  @ViewChild('messageList') private messageList?: ElementRef<HTMLElement>;

  protected screenStep: ScreenStep = 'nickname';
  protected roomActionMode: RoomActionMode = 'none';

  protected nicknameInput = '';
  protected nickname = '';
  protected nicknameError = '';

  protected createRoomNameInput = '';
  protected createRoomCodeInput = '';
  protected searchRoomCodeInput = '';

  protected roomError = '';
  protected roomInfo = '';

  protected joinedRooms: ChatRoom[] = [];
  protected activeRoomCode: string | null = null;

  protected draft = '';

  private readonly roomCatalog: RoomCatalogEntry[] = [
    { code: 'WS-INTRO', name: 'websocket-intro' },
    { code: 'ANGULAR-CHAT', name: 'angular-chat' }
  ];

  protected get activeRoom(): ChatRoom | null {
    return this.joinedRooms.find((room) => room.code === this.activeRoomCode) ?? null;
  }

  protected enterChat(): void {
    const trimmedNickname = this.nicknameInput.trim();
    if (trimmedNickname.length < 2) {
      this.nicknameError = 'Digite um apelido com pelo menos 2 caracteres.';
      return;
    }

    this.nickname = trimmedNickname;
    this.screenStep = 'chat';
    this.nicknameError = '';
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

  protected createRoom(): void {
    const roomName = this.createRoomNameInput.trim();
    const roomCode = this.createRoomCodeInput.trim().toUpperCase();

    if (roomName.length < 2 || roomCode.length < 3) {
      this.roomError = 'Informe nome (2+) e codigo (3+) para criar a sala.';
      return;
    }

    if (this.roomCatalog.some((room) => room.code === roomCode)) {
      this.roomError = `Ja existe uma sala com o codigo ${roomCode}.`;
      return;
    }

    const newCatalogEntry: RoomCatalogEntry = {
      code: roomCode,
      name: roomName
    };

    this.roomCatalog.push(newCatalogEntry);
    this.joinCatalogRoom(newCatalogEntry);

    this.createRoomNameInput = '';
    this.createRoomCodeInput = '';
    this.roomActionMode = 'none';
    this.roomError = '';
    this.roomInfo = `Sala #${roomCode} criada e aberta.`;
  }

  protected searchRoom(): void {
    const roomCode = this.searchRoomCodeInput.trim().toUpperCase();

    if (roomCode.length < 3) {
      this.roomError = 'Digite um codigo de sala valido.';
      return;
    }

    const catalogRoom = this.roomCatalog.find((room) => room.code === roomCode);
    if (!catalogRoom) {
      this.roomError = `Sala com codigo ${roomCode} nao encontrada.`;
      return;
    }

    this.joinCatalogRoom(catalogRoom);
    this.searchRoomCodeInput = '';
    this.roomActionMode = 'none';
    this.roomError = '';
    this.roomInfo = `Sala #${roomCode} aberta.`;
  }

  protected selectRoom(code: string): void {
    this.activeRoomCode = code;
    this.scrollToBottom();
  }

  protected sendMessage(): void {
    const room = this.activeRoom;
    const messageBody = this.draft.trim();

    if (!room || !messageBody) {
      return;
    }

    room.messages = [
      ...room.messages,
      {
        id: room.nextMessageId,
        author: this.nickname,
        body: messageBody,
        createdAt: new Date(),
        mine: true
      }
    ];

    room.nextMessageId += 1;
    this.draft = '';
    this.scrollToBottom();
  }

  protected handleComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private joinCatalogRoom(catalogRoom: RoomCatalogEntry): void {
    const existingRoom = this.joinedRooms.find((room) => room.code === catalogRoom.code);
    if (existingRoom) {
      this.activeRoomCode = existingRoom.code;
      this.scrollToBottom();
      return;
    }

    const messages: ChatMessage[] = [
      {
        id: 1,
        author: 'Sistema',
        body: `Voce entrou na sala ${catalogRoom.name} (#${catalogRoom.code}).`,
        createdAt: new Date(),
        mine: false
      }
    ];

    const joinedRoom: ChatRoom = {
      code: catalogRoom.code,
      name: catalogRoom.name,
      messages,
      nextMessageId: 2
    };

    this.joinedRooms = [...this.joinedRooms, joinedRoom];
    this.activeRoomCode = joinedRoom.code;
    this.scrollToBottom();
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
