#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface AgeRangeWrapper : NSObject

- (instancetype)initWithLibrary:(void *)library;
- (void)dispatchEvent:(NSDictionary *)eventData;
- (void)dispatchUpdateEvent:(NSDictionary *)eventData;
- (void)dispatchCommunicationEvent:(NSDictionary *)eventData;

@end

@interface MainSwift : NSObject

- (instancetype)initWithWrapper:(AgeRangeWrapper *)wrapper;
- (void)requestAgeRangeWithGate1:(NSInteger)gate1
                           gate2:(NSInteger)gate2
                           gate3:(NSInteger)gate3
                  viewController:(UIViewController *)viewController;
- (void)requestSignificantUpdatePermissionWithDescription:(NSString *)description
                                           viewController:(UIViewController *)viewController;
- (void)requestCommunicationPermissionWithHandle:(NSString *)handle
                                      handleKind:(NSString *)handleKind
                                  viewController:(UIViewController *)viewController;
- (void)startListeningForCommunicationResponses;

@end
