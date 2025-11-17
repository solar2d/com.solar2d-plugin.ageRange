#import "AgeRange.h"
#include <CoronaRuntime.h>
#import <UIKit/UIKit.h>

// Forward declaration
class AgeRange;

// ----------------------------------------------------------------------------
static MainSwift * mainSwift = nil;
static AgeRangeWrapper *wrapper = nil;
class AgeRange
{
    public:
        typedef AgeRange Self;

    public:
        static const char kName[];
        static const char kEvent[];
        static const char kUpdateEvent[];
        static const char kCommunicationEvent[];

    protected:
        AgeRange();

    public:
        bool Initialize( CoronaLuaRef listener );
        bool InitializeUpdateListener( CoronaLuaRef listener );
        bool InitializeCommunicationListener( CoronaLuaRef listener );

    public:
        CoronaLuaRef GetListener() const { return fListener; }
        lua_State* GetLuaState() const { return fLuaState; }

    public:
        static int Open( lua_State *L );

    protected:
        static int Finalizer( lua_State *L );

    public:
        static Self *ToLibrary( lua_State *L );

    public:
        static int init( lua_State *L );
        static int requestAgeRange( lua_State *L );
        static int requestSignificantUpdatePermission( lua_State *L );
        static int requestCommunicationPermission( lua_State *L );
        static int startListeningForCommunicationResponses( lua_State *L );
        void DispatchEvent(NSDictionary *eventData);
        void DispatchUpdateEvent(NSDictionary *eventData);
        void DispatchCommunicationEvent(NSDictionary *eventData);

    private:
        CoronaLuaRef fListener;
        CoronaLuaRef fUpdateListener;
        CoronaLuaRef fCommunicationListener;
        lua_State *fLuaState;
};

// ----------------------------------------------------------------------------
// Objective-C Wrapper Implementation
@implementation AgeRangeWrapper {
    AgeRange *_library;
}

- (instancetype)initWithLibrary:(void *)library {
    self = [super init];
    if (self) {
        _library = (AgeRange *)library;
    }
    return self;
}

- (void)dispatchEvent:(NSDictionary *)eventData {
    if (_library) {
        _library->DispatchEvent(eventData);
    }
}

- (void)dispatchUpdateEvent:(NSDictionary *)eventData {
    if (_library) {
        _library->DispatchUpdateEvent(eventData);
    }
}

- (void)dispatchCommunicationEvent:(NSDictionary *)eventData {
    if (_library) {
        _library->DispatchCommunicationEvent(eventData);
    }
}

@end

// ----------------------------------------------------------------------------

const char AgeRange::kName[] = "plugin.ageRange";
const char AgeRange::kEvent[] = "ageRangeEvent";
const char AgeRange::kUpdateEvent[] = "significantUpdateEvent";
const char AgeRange::kCommunicationEvent[] = "communicationEvent";

AgeRange::AgeRange()
:    fListener( NULL )
,    fUpdateListener( NULL )
,    fCommunicationListener( NULL )
,    fLuaState( NULL )
{
}

bool
AgeRange::Initialize( CoronaLuaRef listener )
{
    bool result = ( NULL == fListener );
    if ( result )
    {
        fListener = listener;
    }
    return result;
}

bool
AgeRange::InitializeUpdateListener( CoronaLuaRef listener )
{
    bool result = ( NULL == fUpdateListener );
    if ( result )
    {
        fUpdateListener = listener;
    }
    return result;
}

bool
AgeRange::InitializeCommunicationListener( CoronaLuaRef listener )
{
    bool result = ( NULL == fCommunicationListener );
    if ( result )
    {
        fCommunicationListener = listener;
    }
    return result;
}

int
AgeRange::Open( lua_State *L )
{
    const char kMetatableName[] = __FILE__;
    CoronaLuaInitializeGCMetatable( L, kMetatableName, Finalizer );

    const luaL_Reg kVTable[] =
    {
        { "init", init },
        { "requestAgeRange", requestAgeRange },
        { "requestSignificantUpdatePermission", requestSignificantUpdatePermission },
        { "requestCommunicationPermission", requestCommunicationPermission },
        { "startListeningForCommunicationResponses", startListeningForCommunicationResponses },

        { NULL, NULL }
    };

    Self *library = new Self;
    library->fLuaState = L;
    
    wrapper = [[AgeRangeWrapper alloc] initWithLibrary:library];
    if (mainSwift == nil) {
        mainSwift = [[MainSwift alloc] initWithWrapper:wrapper];
    }
    
    CoronaLuaPushUserdata( L, library, kMetatableName );
    luaL_openlib( L, kName, kVTable, 1 );

    
    return 1;
}

int
AgeRange::Finalizer( lua_State *L )
{
    Self *library = (Self *)CoronaLuaToUserdata( L, 1 );

    CoronaLuaDeleteRef( L, library->GetListener() );

    delete library;

    return 0;
}

AgeRange *
AgeRange::ToLibrary( lua_State *L )
{
    Self *library = (Self *)CoronaLuaToUserdata( L, lua_upvalueindex( 1 ) );
    return library;
}

void
AgeRange::DispatchEvent(NSDictionary *eventData)
{
    if (fListener == NULL || fLuaState == NULL) {
        return;
    }
    
    CoronaLuaNewEvent(fLuaState, kEvent);
    
    if (eventData[@"isError"]) {
        lua_pushboolean(fLuaState, [eventData[@"isError"] boolValue]);
        lua_setfield(fLuaState, -2, "isError");
    }
    
    if (eventData[@"errorMessage"]) {
        lua_pushstring(fLuaState, [eventData[@"errorMessage"] UTF8String]);
        lua_setfield(fLuaState, -2, "errorMessage");
    }
    
    if (eventData[@"lowerBound"]) {
        lua_pushinteger(fLuaState, [eventData[@"lowerBound"] integerValue]);
        lua_setfield(fLuaState, -2, "lowerBound");
    }
    
    if (eventData[@"upperBound"]) {
        lua_pushinteger(fLuaState, [eventData[@"upperBound"] integerValue]);
        lua_setfield(fLuaState, -2, "upperBound");
    }
    
    if (eventData[@"isAvailable"]) {
        lua_pushboolean(fLuaState, [eventData[@"isAvailable"] boolValue]);
        lua_setfield(fLuaState, -2, "isAvailable");
    }
    
    if (eventData[@"declined"]) {
        lua_pushboolean(fLuaState, [eventData[@"declined"] boolValue]);
        lua_setfield(fLuaState, -2, "declined");
    }
    
    if (eventData[@"hasParentalControls"]) {
        lua_pushboolean(fLuaState, [eventData[@"hasParentalControls"] boolValue]);
        lua_setfield(fLuaState, -2, "hasParentalControls");
    }
    
    if (eventData[@"userStatus"]) {
        lua_pushstring(fLuaState, [eventData[@"userStatus"] UTF8String]);
        lua_setfield(fLuaState, -2, "userStatus");
    }
    
    CoronaLuaDispatchEvent(fLuaState, fListener, 0);
}

void
AgeRange::DispatchUpdateEvent(NSDictionary *eventData)
{
    if (fListener == NULL || fLuaState == NULL) {
        return;
    }
    
    CoronaLuaNewEvent(fLuaState, kUpdateEvent);
    
    if (eventData[@"isError"]) {
        lua_pushboolean(fLuaState, [eventData[@"isError"] boolValue]);
        lua_setfield(fLuaState, -2, "isError");
    }
    
    if (eventData[@"errorMessage"]) {
        lua_pushstring(fLuaState, [eventData[@"errorMessage"] UTF8String]);
        lua_setfield(fLuaState, -2, "errorMessage");
    }
    
    if (eventData[@"approved"]) {
        lua_pushboolean(fLuaState, [eventData[@"approved"] boolValue]);
        lua_setfield(fLuaState, -2, "approved");
    }
    
    if (eventData[@"questionSent"]) {
        lua_pushboolean(fLuaState, [eventData[@"questionSent"] boolValue]);
        lua_setfield(fLuaState, -2, "questionSent");
    }
    
    if (eventData[@"description"]) {
        lua_pushstring(fLuaState, [eventData[@"description"] UTF8String]);
        lua_setfield(fLuaState, -2, "description");
    }
    
    CoronaLuaDispatchEvent(fLuaState, fListener, 0);
}

void
AgeRange::DispatchCommunicationEvent(NSDictionary *eventData)
{
    if (fListener == NULL || fLuaState == NULL) {
        return;
    }
    
    CoronaLuaNewEvent(fLuaState, kCommunicationEvent);
    
    if (eventData[@"isError"]) {
        lua_pushboolean(fLuaState, [eventData[@"isError"] boolValue]);
        lua_setfield(fLuaState, -2, "isError");
    }
    
    if (eventData[@"errorMessage"]) {
        lua_pushstring(fLuaState, [eventData[@"errorMessage"] UTF8String]);
        lua_setfield(fLuaState, -2, "errorMessage");
    }
    
    if (eventData[@"approved"]) {
        lua_pushboolean(fLuaState, [eventData[@"approved"] boolValue]);
        lua_setfield(fLuaState, -2, "approved");
    }
    
    if (eventData[@"handle"]) {
        lua_pushstring(fLuaState, [eventData[@"handle"] UTF8String]);
        lua_setfield(fLuaState, -2, "handle");
    }
    
    if (eventData[@"questionSent"]) {
        lua_pushboolean(fLuaState, [eventData[@"questionSent"] boolValue]);
        lua_setfield(fLuaState, -2, "questionSent");
    }
    
    if (eventData[@"isBackgroundResponse"]) {
        lua_pushboolean(fLuaState, [eventData[@"isBackgroundResponse"] boolValue]);
        lua_setfield(fLuaState, -2, "isBackgroundResponse");
    }
    
    CoronaLuaDispatchEvent(fLuaState, fListener, 0);
}

// ----------------------------------------------------------------------------
// Lua API Functions
// ----------------------------------------------------------------------------

int
AgeRange::init( lua_State *L )
{
    Self *library = ToLibrary(L);
    
    if (CoronaLuaIsListener(L, 1, kEvent)) {
        CoronaLuaRef listener = CoronaLuaNewRef(L, 1);
        library->Initialize(listener);
    }

    return 0;
}

int
AgeRange::requestAgeRange( lua_State *L )
{
    
    void *platformContext = CoronaLuaGetContext( L );
    id<CoronaRuntime> runtime = (__bridge id<CoronaRuntime>)platformContext;
    UIViewController *root = runtime.appViewController;
    
    int gate1 = 13;
    int gate2 = 15;
    int gate3 = 18;
    
    if (lua_isnumber(L, 1)) {
        gate1 = (int)lua_tointeger(L, 1);
    }
    if (lua_isnumber(L, 2)) {
        gate2 = (int)lua_tointeger(L, 2);
    }
    if (lua_isnumber(L, 3)) {
        gate3 = (int)lua_tointeger(L, 3);
    }
    
    [mainSwift requestAgeRangeWithGate1:gate1 gate2:gate2 gate3:gate3 viewController:root];

    return 0;
}

int
AgeRange::requestSignificantUpdatePermission( lua_State *L )
{
    void *platformContext = CoronaLuaGetContext( L );
    id<CoronaRuntime> runtime = (__bridge id<CoronaRuntime>)platformContext;
    UIViewController *root = runtime.appViewController;
    
    const char *description = lua_tostring(L, 1);
    if (description == NULL) {
        luaL_error(L, "Description is required for significant update permission");
        return 0;
    }
    
    NSString *updateDescription = [NSString stringWithUTF8String:description];
    
    [mainSwift requestSignificantUpdatePermissionWithDescription:updateDescription viewController:root];

    return 0;
}

int
AgeRange::requestCommunicationPermission( lua_State *L )
{
    
    void *platformContext = CoronaLuaGetContext( L );
    id<CoronaRuntime> runtime = (__bridge id<CoronaRuntime>)platformContext;
    UIViewController *root = runtime.appViewController;
    
    const char *handle = lua_tostring(L, 1);
    if (handle == NULL) {
        luaL_error(L, "Handle is required for communication permission");
        return 0;
    }
    
    NSString *handleValue = [NSString stringWithUTF8String:handle];
    
    // Optional: handle kind (default: custom)
    const char *handleKind = "custom";
    if (lua_isstring(L, 2)) {
        handleKind = lua_tostring(L, 2);
    }
    NSString *handleKindValue = [NSString stringWithUTF8String:handleKind];
    
    [mainSwift requestCommunicationPermissionWithHandle:handleValue handleKind:handleKindValue viewController:root];

    return 0;
}

int
AgeRange::startListeningForCommunicationResponses( lua_State *L )
{
    [mainSwift startListeningForCommunicationResponses];
    return 0;
}

// ----------------------------------------------------------------------------

CORONA_EXPORT int luaopen_plugin_ageRange( lua_State *L )
{
    return AgeRange::Open( L );
}
