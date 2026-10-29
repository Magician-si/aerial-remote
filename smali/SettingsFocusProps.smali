.class public final Lapp/orchard/tv/remote/SettingsFocusProps;
.super Ljava/lang/Object;
.source "SettingsFocusProps"

# interfaces
.implements Li3/c;


# instance fields
.field public final a:LR/t;

.field public final b:LR/t;


# direct methods
.method public constructor <init>(LR/t;LR/t;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lapp/orchard/tv/remote/SettingsFocusProps;->a:LR/t;

    iput-object p2, p0, Lapp/orchard/tv/remote/SettingsFocusProps;->b:LR/t;

    return-void
.end method


# virtual methods
.method public final a(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 2

    check-cast p1, LR/n;

    iget-object v0, p0, Lapp/orchard/tv/remote/SettingsFocusProps;->b:LR/t;

    if-eqz v0, :no_right

    invoke-interface {p1, v0}, LR/n;->e(LR/t;)V

    :no_right
    iget-object v0, p0, Lapp/orchard/tv/remote/SettingsFocusProps;->a:LR/t;

    if-eqz v0, :done

    instance-of v1, p1, LR/p;

    if-eqz v1, :done

    check-cast p1, LR/p;

    iput-object v0, p1, LR/p;->f:LR/t;

    :done
    const/4 v0, 0x0

    return-object v0
.end method
