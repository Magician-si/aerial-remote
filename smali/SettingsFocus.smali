.class public final Lapp/orchard/tv/remote/SettingsFocus;
.super Ljava/lang/Object;
.source "SettingsFocus"


# static fields
.field public static req0:LR/t;

.field public static req1:LR/t;

.field public static req2:LR/t;

.field public static lam0:Li3/c;

.field public static lam1:Li3/c;

.field public static lam2:Li3/c;


# direct methods
.method static constructor <clinit>()V
    .locals 3

    new-instance v0, LR/t;

    invoke-direct {v0}, LR/t;-><init>()V

    sput-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req0:LR/t;

    new-instance v0, LR/t;

    invoke-direct {v0}, LR/t;-><init>()V

    sput-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req1:LR/t;

    new-instance v0, LR/t;

    invoke-direct {v0}, LR/t;-><init>()V

    sput-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req2:LR/t;

    sget-object v1, Lapp/orchard/tv/remote/SettingsFocus;->req1:LR/t;

    new-instance v0, Lapp/orchard/tv/remote/SettingsFocusProps;

    const/4 v2, 0x0

    invoke-direct {v0, v2, v1}, Lapp/orchard/tv/remote/SettingsFocusProps;-><init>(LR/t;LR/t;)V

    sput-object v0, Lapp/orchard/tv/remote/SettingsFocus;->lam0:Li3/c;

    sget-object v1, Lapp/orchard/tv/remote/SettingsFocus;->req0:LR/t;

    sget-object v2, Lapp/orchard/tv/remote/SettingsFocus;->req2:LR/t;

    new-instance v0, Lapp/orchard/tv/remote/SettingsFocusProps;

    invoke-direct {v0, v1, v2}, Lapp/orchard/tv/remote/SettingsFocusProps;-><init>(LR/t;LR/t;)V

    sput-object v0, Lapp/orchard/tv/remote/SettingsFocus;->lam1:Li3/c;

    sget-object v1, Lapp/orchard/tv/remote/SettingsFocus;->req1:LR/t;

    new-instance v0, Lapp/orchard/tv/remote/SettingsFocusProps;

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Lapp/orchard/tv/remote/SettingsFocusProps;-><init>(LR/t;LR/t;)V

    sput-object v0, Lapp/orchard/tv/remote/SettingsFocus;->lam2:Li3/c;

    return-void
.end method

.method public static req(I)LR/t;
    .locals 1

    if-eqz p0, :r0

    const/4 v0, 0x1

    if-eq p0, v0, :r1

    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req2:LR/t;

    return-object v0

    :r0
    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req0:LR/t;

    return-object v0

    :r1
    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req1:LR/t;

    return-object v0
.end method

.method public static props(LR/t;)Li3/c;
    .locals 1

    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req0:LR/t;

    if-eq v0, p0, :is0

    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req1:LR/t;

    if-eq v0, p0, :is1

    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->req2:LR/t;

    if-eq v0, p0, :is2

    const/4 v0, 0x0

    return-object v0

    :is0
    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->lam0:Li3/c;

    return-object v0

    :is1
    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->lam1:Li3/c;

    return-object v0

    :is2
    sget-object v0, Lapp/orchard/tv/remote/SettingsFocus;->lam2:Li3/c;

    return-object v0
.end method
