import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { API_ENDPOINTS } from '../api/endpoints';
import { apiConfig } from '../config/api.config';
import { MessageResponse, SendMessageRequest } from '../models/message.model';

type MessageHandler = (message: MessageResponse) => void;
type PresenceHandler = (payload: unknown) => void;
type OwnershipHandler = (payload: unknown) => void;

interface PresenceRequest {
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class ChatWebSocketService implements OnDestroy {
  private client: Client | null = null;
  private connectionPromise: Promise<void> | null = null;
  private isConnected = false;

  private readonly messageHandlersByTopic = new Map<string, Set<MessageHandler>>();
  private readonly presenceHandlersByTopic = new Map<string, Set<PresenceHandler>>();
  private readonly ownershipHandlersByTopic = new Map<string, Set<OwnershipHandler>>();

  private readonly messageSubscriptionsByTopic = new Map<string, StompSubscription>();
  private readonly presenceSubscriptionsByTopic = new Map<string, StompSubscription>();
  private readonly ownershipSubscriptionsByTopic = new Map<string, StompSubscription>();

  async connect(): Promise<void> {
    if (this.isConnected) {
      return;
    }

    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    this.client = new Client({
      brokerURL: this.resolveBrokerUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    this.connectionPromise = new Promise<void>((resolve, reject) => {
      if (!this.client) {
        reject(new Error('WebSocket client is not initialized.'));
        return;
      }

      this.client.onConnect = () => {
        this.isConnected = true;
        this.resubscribeAllTopics();
        resolve();
      };

      this.client.onStompError = (frame) => {
        if (!this.isConnected) {
          reject(new Error(frame.headers['message'] || 'WebSocket STOMP error.'));
        }
      };

      this.client.onWebSocketError = () => {
        if (!this.isConnected) {
          reject(new Error('Unable to connect to realtime WebSocket endpoint.'));
        }
      };

      this.client.onDisconnect = () => {
        this.isConnected = false;
        this.messageSubscriptionsByTopic.clear();
        this.presenceSubscriptionsByTopic.clear();
        this.ownershipSubscriptionsByTopic.clear();
      };
    });

    this.client.activate();

    try {
      await this.connectionPromise;
    } finally {
      this.connectionPromise = null;
    }
  }

  async sendRoomMessage(roomId: string, payload: SendMessageRequest): Promise<void> {
    await this.connect();

    if (!this.client || !this.isConnected) {
      throw new Error('Realtime connection is not available.');
    }

    this.client.publish({
      destination: API_ENDPOINTS.websocket.appSendRoomMessage(roomId),
      headers: {
        'content-type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
  }

  async joinRoomPresence(roomId: string, userId: string): Promise<void> {
    await this.publishPresence(API_ENDPOINTS.websocket.appJoinRoomPresence(roomId), { userId });
  }

  async leaveRoomPresence(roomId: string, userId: string): Promise<void> {
    await this.publishPresence(API_ENDPOINTS.websocket.appLeaveRoomPresence(roomId), { userId });
  }

  async subscribeRoomMessages(roomId: string, handler: MessageHandler): Promise<() => void> {
    const topic = API_ENDPOINTS.websocket.topicRoomMessages(roomId);

    const handlers = this.messageHandlersByTopic.get(topic) ?? new Set<MessageHandler>();
    handlers.add(handler);
    this.messageHandlersByTopic.set(topic, handlers);

    await this.connect();
    this.ensureMessageSubscription(topic);

    return () => {
      const currentHandlers = this.messageHandlersByTopic.get(topic);
      if (!currentHandlers) {
        return;
      }

      currentHandlers.delete(handler);
      if (currentHandlers.size > 0) {
        return;
      }

      this.messageHandlersByTopic.delete(topic);
      this.messageSubscriptionsByTopic.get(topic)?.unsubscribe();
      this.messageSubscriptionsByTopic.delete(topic);
    };
  }

  async subscribeRoomPresence(roomId: string, handler: PresenceHandler): Promise<() => void> {
    const topic = API_ENDPOINTS.websocket.topicRoomPresence(roomId);

    const handlers = this.presenceHandlersByTopic.get(topic) ?? new Set<PresenceHandler>();
    handlers.add(handler);
    this.presenceHandlersByTopic.set(topic, handlers);

    await this.connect();
    this.ensurePresenceSubscription(topic);

    return () => {
      const currentHandlers = this.presenceHandlersByTopic.get(topic);
      if (!currentHandlers) {
        return;
      }

      currentHandlers.delete(handler);
      if (currentHandlers.size > 0) {
        return;
      }

      this.presenceHandlersByTopic.delete(topic);
      this.presenceSubscriptionsByTopic.get(topic)?.unsubscribe();
      this.presenceSubscriptionsByTopic.delete(topic);
    };
  }

  async subscribeRoomOwnership(roomId: string, handler: OwnershipHandler): Promise<() => void> {
    const topic = API_ENDPOINTS.websocket.topicRoomOwnership(roomId);

    const handlers = this.ownershipHandlersByTopic.get(topic) ?? new Set<OwnershipHandler>();
    handlers.add(handler);
    this.ownershipHandlersByTopic.set(topic, handlers);

    await this.connect();
    this.ensureOwnershipSubscription(topic);

    return () => {
      const currentHandlers = this.ownershipHandlersByTopic.get(topic);
      if (!currentHandlers) {
        return;
      }

      currentHandlers.delete(handler);
      if (currentHandlers.size > 0) {
        return;
      }

      this.ownershipHandlersByTopic.delete(topic);
      this.ownershipSubscriptionsByTopic.get(topic)?.unsubscribe();
      this.ownershipSubscriptionsByTopic.delete(topic);
    };
  }

  disconnect(): void {
    this.messageHandlersByTopic.clear();
    this.presenceHandlersByTopic.clear();
    this.ownershipHandlersByTopic.clear();

    for (const subscription of this.messageSubscriptionsByTopic.values()) {
      subscription.unsubscribe();
    }
    this.messageSubscriptionsByTopic.clear();

    for (const subscription of this.presenceSubscriptionsByTopic.values()) {
      subscription.unsubscribe();
    }
    this.presenceSubscriptionsByTopic.clear();

    for (const subscription of this.ownershipSubscriptionsByTopic.values()) {
      subscription.unsubscribe();
    }
    this.ownershipSubscriptionsByTopic.clear();

    this.client?.deactivate();
    this.client = null;
    this.connectionPromise = null;
    this.isConnected = false;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  private ensureMessageSubscription(topic: string): void {
    if (!this.client || !this.isConnected || this.messageSubscriptionsByTopic.has(topic)) {
      return;
    }

    const subscription = this.client.subscribe(topic, (messageFrame: IMessage) => {
      const payload = this.parseMessage(messageFrame.body);
      if (!payload) {
        return;
      }

      const handlers = this.messageHandlersByTopic.get(topic);
      if (!handlers) {
        return;
      }

      for (const handler of handlers) {
        handler(payload);
      }
    });

    this.messageSubscriptionsByTopic.set(topic, subscription);
  }

  private ensurePresenceSubscription(topic: string): void {
    if (!this.client || !this.isConnected || this.presenceSubscriptionsByTopic.has(topic)) {
      return;
    }

    const subscription = this.client.subscribe(topic, (messageFrame: IMessage) => {
      const payload = this.parseJson(messageFrame.body);
      if (payload === null) {
        return;
      }

      const handlers = this.presenceHandlersByTopic.get(topic);
      if (!handlers) {
        return;
      }

      for (const handler of handlers) {
        handler(payload);
      }
    });

    this.presenceSubscriptionsByTopic.set(topic, subscription);
  }

  private ensureOwnershipSubscription(topic: string): void {
    if (!this.client || !this.isConnected || this.ownershipSubscriptionsByTopic.has(topic)) {
      return;
    }

    const subscription = this.client.subscribe(topic, (messageFrame: IMessage) => {
      const payload = this.parseJson(messageFrame.body);
      if (payload === null) {
        return;
      }

      const handlers = this.ownershipHandlersByTopic.get(topic);
      if (!handlers) {
        return;
      }

      for (const handler of handlers) {
        handler(payload);
      }
    });

    this.ownershipSubscriptionsByTopic.set(topic, subscription);
  }

  private resubscribeAllTopics(): void {
    for (const topic of this.messageHandlersByTopic.keys()) {
      this.ensureMessageSubscription(topic);
    }

    for (const topic of this.presenceHandlersByTopic.keys()) {
      this.ensurePresenceSubscription(topic);
    }

    for (const topic of this.ownershipHandlersByTopic.keys()) {
      this.ensureOwnershipSubscription(topic);
    }
  }

  private parseMessage(rawBody: string): MessageResponse | null {
    const parsed = this.parseJson(rawBody);
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }

    return parsed as MessageResponse;
  }

  private parseJson(rawBody: string): unknown | null {
    try {
      return JSON.parse(rawBody);
    } catch {
      return null;
    }
  }

  private async publishPresence(destination: string, payload: PresenceRequest): Promise<void> {
    await this.connect();

    if (!this.client || !this.isConnected) {
      throw new Error('Realtime connection is not available.');
    }

    this.client.publish({
      destination,
      headers: {
        'content-type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
  }

  private resolveBrokerUrl(): string {
    const wsBaseUrl = apiConfig.websocketBaseUrl.replace(/^http/i, 'ws');
    return `${wsBaseUrl}${API_ENDPOINTS.websocket.endpoint}`;
  }
}
