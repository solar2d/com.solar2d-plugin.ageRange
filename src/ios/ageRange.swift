import Foundation
import UIKit
import PermissionKit
import DeclaredAgeRange

@available(iOS 13.0, *)
@objc(MainSwift)
class MainSwift: NSObject {
    
    private weak var wrapper: AgeRangeWrapper?
    private var responseTask: Task<Void, Never>?
    
    @objc init(wrapper: AgeRangeWrapper) {
        self.wrapper = wrapper
        super.init()
    }
    
    // MARK: - Age Range
    
    @available(iOS 26.0, tvOS 26.0, *)
    @MainActor
    @objc func requestAgeRange(gate1: Int, gate2: Int, gate3: Int, viewController: UIViewController) {
        Task {
            do {
                let response = try await AgeRangeService.shared.requestAgeRange(
                    ageGates: gate1, gate2, gate3,
                    in: viewController
                )
                
                switch response {
                case .declinedSharing:
                    let eventData: [String: Any] = [
                        "isError": false,
                        "isAvailable": false,
                        "declined": true,
                        "userStatus": "declined",
                        "errorMessage": "User declined to share age range"
                    ]
                    self.dispatchEvent(eventData: eventData)
                    
                case .sharing(let range):
                    var eventData: [String: Any] = [
                        "isError": false,
                        "isAvailable": true,
                        "declined": false
                    ]
                    
                    if let lowerBound = range.lowerBound {
                        eventData["lowerBound"] = lowerBound
                    }
                    
                    if let upperBound = range.upperBound {
                        eventData["upperBound"] = upperBound
                    }
                    
                    // Determine user status based on parental controls and age
                    if range.activeParentalControls.isEmpty {
                        // No parental controls - likely verified adult (18+)
                        eventData["hasParentalControls"] = false
                        eventData["userStatus"] = "verified"
                    } else {
                        // Has parental controls - supervised user
                        eventData["hasParentalControls"] = true
                        
                        // Check for communication limits to determine supervision status
                        if range.activeParentalControls.contains(.communicationLimits) {
                            eventData["userStatus"] = "supervised"
                        } else {
                            eventData["userStatus"] = "supervised"
                        }
                    }
                    
                    self.dispatchEvent(eventData: eventData)
                @unknown default: break
                    
                }
                
            } catch AgeRangeService.Error.notAvailable {
                let eventData: [String: Any] = [
                    "isError": false,
                    "isAvailable": false,
                    "declined": false,
                    "userStatus": "notAvailable",
                    "errorMessage": "Age range service not available"
                ]
                self.dispatchEvent(eventData: eventData)
                
            } catch {
                let eventData: [String: Any] = [
                    "isError": true,
                    "isAvailable": false,
                    "declined": false,
                    "userStatus": "error",
                    "errorMessage": error.localizedDescription
                ]
                self.dispatchEvent(eventData: eventData)
            }
        }
    }
    
    // MARK: - Significant Update Permission
    
    @available(iOS 26.2, *)
    @MainActor
    @objc func requestSignificantUpdatePermission(description: String, viewController: UIViewController) {
        Task {
            do {
                // Create a significant app update topic
                let topic = SignificantAppUpdateTopic(description: description)
                let question = PermissionQuestion<SignificantAppUpdateTopic>(significantAppUpdateTopic: topic)
                
                // Send the question - this triggers the Messages flow
                try await AskCenter.shared.ask(question, in: viewController)
                
                // The ask method completes when the question is sent, not when answered
                let eventData: [String: Any] = [
                    "isError": false,
                    "description": description,
                    "questionSent": true
                ]
                
                self.dispatchUpdateEvent(eventData: eventData)
                
            } catch {
                let eventData: [String: Any] = [
                    "isError": true,
                    "approved": false,
                    "description": description,
                    "errorMessage": error.localizedDescription
                ]
                self.dispatchUpdateEvent(eventData: eventData)
            }
        }
    }
    
    // MARK: - Communication Permission
    
    @available(iOS 26.2, tvOS 26.2, *)
    @MainActor
    @objc func requestCommunicationPermission(handle: String, handleKind: String, viewController: UIViewController) {
        Task {
            do {
                let handleType: CommunicationHandle.Kind
                switch handleKind.lowercased() {
                case "phone":
                    handleType = .phoneNumber
                case "email":
                    handleType = .emailAddress
                default:
                    handleType = .custom
                }
                
                let commHandle = CommunicationHandle(value: handle, kind: handleType)
                
                // Use the convenience initializer for single handle
                let question = PermissionQuestion<CommunicationTopic>(handle: commHandle)
                
                // Send the question using AskCenter
                try await AskCenter.shared.ask(question, in: viewController)
                
                let eventData: [String: Any] = [
                    "isError": false,
                    "handle": handle,
                    "questionSent": true
                ]
                
                self.dispatchCommunicationEvent(eventData: eventData)
                
            } catch {
                let eventData: [String: Any] = [
                    "isError": true,
                    "approved": false,
                    "handle": handle,
                    "errorMessage": error.localizedDescription
                ]
                self.dispatchCommunicationEvent(eventData: eventData)
            }
        }
    }
    
    // MARK: - Listen for Communication Responses
    
    @available(iOS 26.2, tvOS 26.2, *)
    @MainActor
    @objc func startListeningForCommunicationResponses() {
        // Cancel existing task if any
        responseTask?.cancel()
        
        responseTask = Task {
            // Get the async sequence of responses for CommunicationTopic
            let responses = AskCenter.shared.responses(for: CommunicationTopic.self)
            
            for await response in responses {
                guard !Task.isCancelled else { break }
                
                var eventData: [String: Any] = [
                    "isError": false,
                    "isBackgroundResponse": true
                ]
                
                // Access the choice's answer
                switch response.choice.answer {
                case .approval:
                    eventData["approved"] = true
                    
                case .denial:
                    eventData["approved"] = false
                    
                @unknown default:
                    eventData["approved"] = false
                }
                
                // You can also access the original question if needed
                // let originalQuestion = response.question
                
                self.dispatchCommunicationEvent(eventData: eventData)
            }
        }
    }
    
    // MARK: - Dispatch Events
    private func dispatchEvent(eventData: [String: Any]) {
        wrapper?.dispatchEvent(eventData as [AnyHashable: Any])
    }
    
    private func dispatchUpdateEvent(eventData: [String: Any]) {
        wrapper?.dispatchUpdateEvent(eventData as [AnyHashable: Any])
    }
    
    private func dispatchCommunicationEvent(eventData: [String: Any]) {
        wrapper?.dispatchCommunicationEvent(eventData as [AnyHashable: Any])
    }
    
    deinit {
        responseTask?.cancel()
    }
}

