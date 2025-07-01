//
//  ContentView.swift
//  Sample
//
//  Created by Brahyam Meneses on 20.06.25.
//

import SwiftUI
import Gateway
import Foundation

struct Message: Identifiable {
    let id = UUID()
    let text: String
    let isFromUser: Bool
}

struct ContentView: View {
    @State private var messages: [Message] = []
    @State private var inputText: String = ""
    @State private var isLoading: Bool = false
    
    private let openAiService = Gateway_.shared.createOpenAIService(
    partialKey: "YOUR_GATEWAY_PARTIAL_KEY", serviceURL: "YOUR_GATEWAY_SERVICE_URL")
    
    var body: some View {
        VStack {
            // Chat messages
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(messages) { message in
                        MessageBubble(message: message)
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 1)
            
            // Loading indicator
            if isLoading {
                ProgressView()
                    .scaleEffect(1.2)
                    .padding()
            }
            
            // Input area
            HStack {
                TextField("Type a message...", text: $inputText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(isLoading)
                
                Button(action: sendMessage) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(.white)
                        .padding(8)
                        .background(Color.blue)
                        .clipShape(Circle())
                }
                .disabled(inputText.isEmpty || isLoading)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .navigationTitle("Chat")
    }
    
    private func sendMessage() {
        guard !inputText.isEmpty && !isLoading else { return }
        
        let userMessage = Message(text: inputText, isFromUser: true)
        messages.append(userMessage)
        
        let currentInput = inputText
        inputText = ""
        isLoading = true
        
        // Send message to OpenAI via Gateway
        Task {
            do {
                let response = try await sendToOpenAI(message: currentInput)
                await MainActor.run {
                    messages.append(Message(text: response, isFromUser: false))
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    messages.append(Message(text: "Error: \(error.localizedDescription)", isFromUser: false))
                    isLoading = false
                }
            }
        }
    }
    
    private func sendToOpenAI(message: String) async throws -> String {
        // Convert existing messages to OpenAI format
        let chatMessages = messages.map { msg in
            Gateway.Openai_coreChatMessage(role: msg.isFromUser ? "user" : "assistant", content: msg.text)
        }
        
        // Add the current user message
        let allMessages = chatMessages + [Gateway.Openai_coreChatMessage(role: "user", content: message)]
        
        let request = Gateway.Openai_coreChatCompletionRequest(model: "gpt-4o-mini", messages: allMessages, reasoningEffort: nil, temperature: nil, topP: nil, n: nil, stop: nil, store: nil, maxTokens: nil, maxCompletionTokens: nil, presencePenalty: nil, frequencyPenalty: nil, logitBias: nil, user: nil, functions: nil, functionCall: nil, responseFormat: nil, tools: nil, toolChoice: nil, seed: nil, logprobs: nil, topLogprobs: nil, instanceId: nil, streamOptions: nil)
        
        // Send request to OpenAI via Gateway
        let response = try await openAiService.chatCompletion(request: request, requestOptions: nil)
        
        // Extract the response text
        guard let firstChoice = response.choices.first,
              let content = firstChoice.message.content else {
            throw NSError(domain: "OpenAI", code: -1, userInfo: [NSLocalizedDescriptionKey: "No response content received"])
        }
        
        return content
    }
}

struct MessageBubble: View {
    let message: Message
    
    var body: some View {
        HStack {
            if message.isFromUser {
                Spacer()
            }
            
            Text(message.text)
                .padding(12)
                .background(message.isFromUser ? Color.blue : Color.gray.opacity(0.2))
                .foregroundColor(message.isFromUser ? .white : .primary)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .frame(maxWidth: 300, alignment: message.isFromUser ? .trailing : .leading)
            
            if !message.isFromUser {
                Spacer()
            }
        }
    }
}

#Preview {
    NavigationView {
        ContentView()
    }
}
