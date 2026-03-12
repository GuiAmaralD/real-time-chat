import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { API_ENDPOINTS } from '../api/endpoints';
import { apiConfig } from '../config/api.config';
import { MessageResponse, SendMessageRequest } from '../models/message.model';

type MessageHandler = (message: MessageResponse) => void;

@Injectable({ providedIn: 'root' })
export class ChatWebSocketService implements OnDestroy {
  private client: Client | null = null;
  private connectionPromise: Promise<void> | null = null;
  private isConnected = false;

  private readonly handlersByTopic = new Map<string, Set<MessageHandler>>();
  private readonly subscriptionsByTopic = new Map<string, StompSubscription>();

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
        this.subscriptionsByTopic.clear();
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

  async subscribeRoomMessages(roomId: string, handler: MessageHandler): Promise<() => void> {
    const topic = API_ENDPOINTS.websocket.topicRoomMessages(roomId);

    const handlers = this.handlersByTopic.get(topic) ?? new Set<MessageHandler>();
    handlers.add(handler);
    this.handlersByTopic.set(topic, handlers);

    await this.connect();
    this.ensureSubscription(topic);

    return () => {
      const currentHandlers = this.handlersByTopic.get(topic);
      if (!currentHandlers) {
        return;
      }

      currentHandlers.delete(handler);
      if (currentHandlers.size > 0) {
        return;
      }

      this.handlersByTopic.delete(topic);
      this.subscriptionsByTopic.get(topic)?.unsubscribe();
      this.subscriptionsByTopic.delete(topic);
    };
  }

  disconnect(): void {
    this.handlersByTopic.clear();

    for (const subscription of this.subscriptionsByTopic.values()) {
      subscription.unsubscribe();
    }
    this.subscriptionsByTopic.clear();

    this.client?.deactivate();
    this.client = null;
    this.connectionPromise = null;
    this.isConnected = false;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  private ensureSubscription(topic: string): void {
    if (!this.client || !this.isConnected || this.subscriptionsByTopic.has(topic)) {
      return;
    }

    const subscription = this.client.subscribe(topic, (messageFrame: IMessage) => {
      const payload = this.parseMessage(messageFrame.body);
      if (!payload) {
        return;
      }

      const handlers = this.handlersByTopic.get(topic);
      if (!handlers) {
        return;
      }

      for (const handler of handlers) {
        handler(payload);
      }
    });

    this.subscriptionsByTopic.set(topic, subscription);
  }

  private resubscribeAllTopics(): void {
    for (const topic of this.handlersByTopic.keys()) {
      this.ensureSubscription(topic);
    }
  }

  private parseMessage(rawBody: string): MessageResponse | null {
    try {
      const parsed = JSON.parse(rawBody) as MessageResponse;
      if (!parsed || typeof parsed !== 'object') {
        return null;
      }

      return parsed;
    } catch {
      return null;
    }
  }

  private resolveBrokerUrl(): string {
    const wsBaseUrl = apiConfig.websocketBaseUrl.replace(/^http/i, 'ws');
    return `${wsBaseUrl}${API_ENDPOINTS.websocket.endpoint}`;
  }
}
