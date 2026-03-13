import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AppComponent } from './app.component';
import { MessageResponse } from './core/models/message.model';
import { RoomResponse, RoomUserResponse } from './core/models/room.model';
import { UserResponse } from './core/models/user.model';
import { ChatWebSocketService } from './core/services/chat-websocket.service';
import { MessageService } from './core/services/message.service';
import { RoomService } from './core/services/room.service';
import { UserService } from './core/services/user.service';

describe('AppComponent', () => {
  let userServiceMock: jasmine.SpyObj<UserService>;
  let roomServiceMock: jasmine.SpyObj<RoomService>;
  let messageServiceMock: jasmine.SpyObj<MessageService>;
  let chatWebSocketServiceMock: jasmine.SpyObj<ChatWebSocketService>;

  beforeEach(async () => {
    userServiceMock = jasmine.createSpyObj<UserService>('UserService', ['create', 'disconnectOnUnload']);
    roomServiceMock = jasmine.createSpyObj<RoomService>('RoomService', [
      'create',
      'joinByCode',
      'findByCode',
      'listUsers'
    ]);
    messageServiceMock = jasmine.createSpyObj<MessageService>('MessageService', ['send', 'listRecent']);
    chatWebSocketServiceMock = jasmine.createSpyObj<ChatWebSocketService>('ChatWebSocketService', [
      'connect',
      'subscribeRoomMessages',
      'sendRoomMessage',
      'subscribeRoomPresence',
      'joinRoomPresence',
      'leaveRoomPresence'
    ]);

    const createdUser: UserResponse = { id: 'user-1', nickname: 'Gui' };
    const createdRoom: RoomResponse = {
      id: 'room-1',
      name: 'My Room',
      code: 'MYROOM123',
      ownerId: 'user-1',
      memberIds: ['user-1']
    };
    const joinedRoom: RoomResponse = {
      id: 'room-2',
      name: 'WS Intro',
      code: 'WS-INTRO',
      ownerId: 'owner-1',
      memberIds: ['owner-1', 'user-1']
    };
    const roomUsers: RoomUserResponse[] = [
      { id: 'owner-1', nickname: 'Owner', role: 'owner' },
      { id: 'user-1', nickname: 'Gui', role: 'member' }
    ];
    const sentMessage: MessageResponse = {
      id: 'message-1',
      roomId: 'room-2',
      userId: 'user-1',
      nickname: 'Gui',
      content: 'WebSocket test',
      sentAt: '2026-03-12T12:00:00Z'
    };
    const roomRealtimeHandlers = new Map<string, (message: MessageResponse) => void>();

    userServiceMock.create.and.returnValue(of(createdUser));
    userServiceMock.disconnectOnUnload.and.stub();
    roomServiceMock.create.and.returnValue(of(createdRoom));
    roomServiceMock.joinByCode.and.returnValue(of(joinedRoom));
    roomServiceMock.listUsers.and.returnValue(of(roomUsers));
    roomServiceMock.findByCode.and.returnValue(of(joinedRoom));
    messageServiceMock.listRecent.and.returnValue(of([]));
    messageServiceMock.send.and.returnValue(of(sentMessage));
    chatWebSocketServiceMock.connect.and.returnValue(Promise.resolve());
    chatWebSocketServiceMock.subscribeRoomMessages.and.callFake(
      async (roomId: string, handler: (message: MessageResponse) => void) => {
        roomRealtimeHandlers.set(roomId, handler);
        return () => {
          roomRealtimeHandlers.delete(roomId);
        };
      }
    );
    chatWebSocketServiceMock.subscribeRoomPresence.and.callFake(async () => () => undefined);
    chatWebSocketServiceMock.joinRoomPresence.and.returnValue(Promise.resolve());
    chatWebSocketServiceMock.leaveRoomPresence.and.returnValue(Promise.resolve());
    chatWebSocketServiceMock.sendRoomMessage.and.callFake(
      async (roomId: string, payload: { userId: string; content: string }) => {
        const handler = roomRealtimeHandlers.get(roomId);
        if (!handler) {
          return;
        }

        handler({
          id: 'message-live-1',
          roomId,
          userId: payload.userId,
          nickname: 'Gui',
          content: payload.content,
          sentAt: '2026-03-12T12:00:00Z'
        });
      }
    );

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: UserService, useValue: userServiceMock },
        { provide: RoomService, useValue: roomServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
        { provide: ChatWebSocketService, useValue: chatWebSocketServiceMock }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should start on nickname screen', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Choose your nickname');
  });

  it('should enter chat after nickname', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    await component.enterChat();

    expect(component.screenStep).toBe('chat');
    expect(component.nickname).toBe('Gui');
    expect(userServiceMock.create).toHaveBeenCalledWith({ nickname: 'Gui' });
    expect(chatWebSocketServiceMock.connect).toHaveBeenCalled();
  });

  it('should create a room with name and code', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    await component.enterChat();

    component.createRoomNameInput = 'My Room';
    component.createRoomCodeInput = 'myroom123';
    await component.createRoom();

    expect(component.joinedRooms.length).toBe(1);
    expect(component.activeRoom?.code).toBe('MYROOM123');
    expect(roomServiceMock.create).toHaveBeenCalledWith({
      name: 'My Room',
      code: 'MYROOM123',
      ownerId: 'user-1'
    });
    expect(chatWebSocketServiceMock.subscribeRoomMessages).toHaveBeenCalledWith(
      'room-1',
      jasmine.any(Function)
    );
  });

  it('should find an existing room by code', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    await component.enterChat();

    component.searchRoomCodeInput = 'ws-intro';
    await component.searchRoom();

    expect(component.joinedRooms.length).toBe(1);
    expect(component.activeRoom?.code).toBe('WS-INTRO');
    expect(roomServiceMock.joinByCode).toHaveBeenCalledWith({
      code: 'WS-INTRO',
      userId: 'user-1'
    });
    expect(chatWebSocketServiceMock.subscribeRoomMessages).toHaveBeenCalledWith(
      'room-2',
      jasmine.any(Function)
    );
  });

  it('should show error when room code is not found', async () => {
    roomServiceMock.joinByCode.and.returnValue(throwError(() => new Error('room not found')));

    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    await component.enterChat();

    component.searchRoomCodeInput = 'not-found';
    await component.searchRoom();

    expect(component.roomError).toContain('room not found');
  });

  it('should send message using global nickname', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    await component.enterChat();

    component.searchRoomCodeInput = 'ws-intro';
    await component.searchRoom();

    const initialLength = component.activeRoom.messages.length;
    component.draft = 'WebSocket test';
    await component.sendMessage();

    expect(component.activeRoom.messages.length).toBe(initialLength + 1);
    expect(component.activeRoom.messages[initialLength].author).toBe('Gui');
    expect(chatWebSocketServiceMock.sendRoomMessage).toHaveBeenCalledWith('room-2', {
      userId: 'user-1',
      content: 'WebSocket test'
    });
    expect(messageServiceMock.send).not.toHaveBeenCalled();
  });
});
