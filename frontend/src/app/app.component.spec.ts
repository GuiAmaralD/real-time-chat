import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent]
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

    expect(compiled.textContent).toContain('Escolha seu apelido');
  });

  it('should enter chat after nickname', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    component.enterChat();

    expect(component.screenStep).toBe('chat');
    expect(component.nickname).toBe('Gui');
  });

  it('should create a room with name and code', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    component.enterChat();

    component.createRoomNameInput = 'Minha Sala';
    component.createRoomCodeInput = 'minha123';
    component.createRoom();

    expect(component.joinedRooms.length).toBe(1);
    expect(component.activeRoom?.code).toBe('MINHA123');
  });

  it('should find an existing room by code', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    component.enterChat();

    component.searchRoomCodeInput = 'ws-intro';
    component.searchRoom();

    expect(component.joinedRooms.length).toBe(1);
    expect(component.activeRoom?.code).toBe('WS-INTRO');
  });

  it('should show error when room code is not found', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    component.enterChat();

    component.searchRoomCodeInput = 'nao-existe';
    component.searchRoom();

    expect(component.roomError).toContain('nao encontrada');
  });

  it('should send message using global nickname', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance as any;

    component.nicknameInput = 'Gui';
    component.enterChat();

    component.searchRoomCodeInput = 'ws-intro';
    component.searchRoom();

    const initialLength = component.activeRoom.messages.length;
    component.draft = 'Teste websocket';
    component.sendMessage();

    expect(component.activeRoom.messages.length).toBe(initialLength + 1);
    expect(component.activeRoom.messages[initialLength].author).toBe('Gui');
  });
});
