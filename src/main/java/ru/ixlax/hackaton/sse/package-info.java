/**
 * Server-Sent Events (SSE)
 *
 * Этот пакет содержит "шину событий" для realtime-уведомлений
 * Когда создаётся новый инцидент, он публикуется в SseHub
 * Подписчики (фронтенд) получают обновления через /api/public/stream
 */
package ru.ixlax.hackaton.sse;