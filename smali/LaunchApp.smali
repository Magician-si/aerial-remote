.class public final synthetic Lapp/orchard/tv/remote/LaunchApp;
.super Ljava/lang/Object;

# interfaces
.implements Li3/a;


# instance fields
.field public final synthetic i:Lu2/f;


# direct methods
.method public synthetic constructor <init>(Lu2/f;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lapp/orchard/tv/remote/LaunchApp;->i:Lu2/f;

    return-void
.end method


# virtual methods
.method public final b()Ljava/lang/Object;
    .locals 4

    iget-object v0, p0, Lapp/orchard/tv/remote/LaunchApp;->i:Lu2/f;

    iget-object v0, v0, Lu2/f;->i:Landroid/content/Intent;

    if-nez v0, :cond_intent_ok

    iget-object v1, p0, Lapp/orchard/tv/remote/LaunchApp;->i:Lu2/f;

    iget-object v1, v1, Lu2/f;->a:Ljava/lang/String;

    sget-object v2, Lapp/orchard/tv/remote/RemoteApp;->appContext:Landroid/content/Context;

    invoke-virtual {v2}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v2

    invoke-virtual {v2, v1}, Landroid/content/pm/PackageManager;->getLaunchIntentForPackage(Ljava/lang/String;)Landroid/content/Intent;

    move-result-object v0

    :cond_intent_ok
    if-nez v0, :cond_launch

    sget-object v0, LT2/p;->a:LT2/p;

    return-object v0

    :cond_launch
    const/high16 v1, 0x10000000

    invoke-virtual {v0, v1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    sget-object v1, Lapp/orchard/tv/remote/RemoteApp;->appContext:Landroid/content/Context;

    invoke-virtual {v1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V

    sget-object v0, LT2/p;->a:LT2/p;

    return-object v0
.end method
