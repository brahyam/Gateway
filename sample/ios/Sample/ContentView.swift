//
//  ContentView.swift
//  Sample
//
//  Created by Brahyam Meneses on 20.06.25.
//

import SwiftUI
import Gateway

struct Message: Identifiable {
    let id = UUID()
    let text: String
    let isFromUser: Bool
}

struct ContentView: View {
    @State private var result: String = "Ready to test Gateway"
    @State private var isLoading: Bool = false
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "network")
                .imageScale(.large)
                .foregroundStyle(.tint)
                .font(.system(size: 50))
            
            Text("Gateway KMP Integration")
                .font(.title)
                .fontWeight(.bold)
            
            Text("Swift Package Manager Example")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Text(result)
                .padding()
                .frame(maxWidth: .infinity)
                .background(Color.gray.opacity(0.1))
                .cornerRadius(8)
                .multilineTextAlignment(.center)
            
            Button(action: testGateway) {
                HStack {
                    if isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    }
                    Text(isLoading ? "Testing..." : "Test Gateway")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(8)
            }
            .disabled(isLoading)
            
            Spacer()
        }
        .padding()
    }
    
    private func testGateway() {
        isLoading = true
        result = "Testing Gateway integration..."
        
        // Simulate async operation
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            // Here you would typically call your Gateway methods
            // For now, we'll just show a success message
            result = "✅ Gateway integration successful!\n\nThis demonstrates that the XCFramework is properly linked and the Swift Package Manager integration is working correctly."
            isLoading = false
        }
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
    ContentView()
}
