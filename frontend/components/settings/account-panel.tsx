"use client";

import { ChangeEvent, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Camera } from "lucide-react";
import { toast } from "sonner";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { authApi } from "@/lib/api/auth";
import { accountMock } from "@/lib/api/account-mock";
import { userApi } from "@/lib/api/user";
import { ApiError } from "@/lib/api/errors";
import { isValidCode, isValidPhone, maskPhone } from "@/lib/validators";
import { useCountdown } from "@/hooks/useCountdown";
import { useAuthStore } from "@/stores/auth";
import { SETTINGS_COPY } from "@/lib/i18n/settings";
import { useLocaleStore } from "@/stores/locale";

const dangerPillClass =
  "h-8 rounded-full border-destructive/40 px-4 text-destructive hover:bg-destructive/10 hover:text-destructive";

const MAX_AVATAR_SIZE = 20 * 1024 * 1024;
const AVATAR_TYPES = new Set(["image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"]);

export function AccountPanel() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const reset = useAuthStore((s) => s.reset);
  const locale = useLocaleStore((s) => s.locale);
  const copy = SETTINGS_COPY[locale];

  const [wechatBound, setWechatBound] = useState(true);
  const [unbinding, setUnbinding] = useState(false);
  const [changeUsernameOpen, setChangeUsernameOpen] = useState(false);
  const [changePhoneOpen, setChangePhoneOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [loggingOutAll, setLoggingOutAll] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  const handleAvatarChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || uploadingAvatar) return;
    if (!AVATAR_TYPES.has(file.type)) {
      toast.error(copy.avatarTypeError);
      return;
    }
    if (file.size > MAX_AVATAR_SIZE) {
      toast.error(copy.avatarSizeError);
      return;
    }

    setUploadingAvatar(true);
    try {
      const avatar = await userApi.updateAvatar(file);
      if (user) setUser({ ...user, avatar });
      toast.success(copy.avatarUpdated);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : copy.avatarUpdateFailed);
    } finally {
      setUploadingAvatar(false);
    }
  };
  const handleUnbindWechat = async () => {
    setUnbinding(true);
    try {
      await accountMock.unbindWechat();
      setWechatBound(false);
      toast.success(copy.wechatUnbound);
    } catch {
      toast.error(copy.unbindFailed);
    } finally {
      setUnbinding(false);
    }
  };

  const handleLogoutAll = async () => {
    setLoggingOutAll(true);
    try {
      const sessions = await authApi.sessions();
      await Promise.all(
        sessions.filter((s) => !s.current).map((s) => authApi.kickSession(s.sessionId)),
      );
      await authApi.logout();
    } catch {
      // 部分设备下线失败也继续清理本地态，保证用户能重新登录
    } finally {
      reset();
      toast.success(copy.loggedOutAll);
      router.replace("/login");
    }
  };

  const handleCancelAccount = async () => {
    setCancelling(true);
    try {
      await accountMock.cancelAccount();
      toast.success(copy.accountCancelled);
      reset();
      router.replace("/login");
    } catch {
      toast.error(copy.cancelFailed);
      setCancelling(false);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between pb-5 pt-1">
        <div className="flex items-center gap-4">
          <Avatar className="size-16">
            <AvatarImage src={user?.avatar || undefined} alt={user?.nickname || copy.avatar} />
            <AvatarFallback className="text-lg">
              {user?.nickname?.[0] ?? "U"}
            </AvatarFallback>
          </Avatar>
          <div className="space-y-1">
            <div className="text-sm font-medium">{copy.avatar}</div>
            <div className="text-xs text-muted-foreground">{copy.avatarHint}</div>
          </div>
        </div>
        <input
          ref={avatarInputRef}
          type="file"
          className="hidden"
          accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
          onChange={handleAvatarChange}
        />
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="shrink-0 gap-1.5"
          disabled={uploadingAvatar}
          onClick={() => avatarInputRef.current?.click()}
        >
          <Camera className="size-4" />
          {uploadingAvatar ? copy.avatarUploading : copy.changeAvatar}
        </Button>
      </div>
      <Separator />
      <div className="flex items-center justify-between py-3">
        <span className="text-sm text-muted-foreground">{copy.username}</span>
        <span className="flex items-center gap-3 text-sm">
          {user?.nickname || "-"}
          <button
            type="button"
            className="text-brand hover:underline"
            onClick={() => setChangeUsernameOpen(true)}
          >
            {copy.change}
          </button>
        </span>
      </div>
      <Separator />
      <div className="flex items-center justify-between py-3">
        <span className="text-sm text-muted-foreground">{copy.phone}</span>
        <span className="flex items-center gap-3 text-sm">
          {maskPhone(user?.phone)}
          <button
            type="button"
            className="text-brand hover:underline"
            onClick={() => setChangePhoneOpen(true)}
          >
            {copy.change}
          </button>
        </span>
      </div>
      <Separator />
      <div className="flex items-center justify-between py-3">
        <span className="text-sm text-muted-foreground">{copy.wechat}</span>
        <span className="flex items-center gap-3 text-sm">
          {wechatBound ? copy.bound : copy.unbound}
          {wechatBound && (
            <button
              type="button"
              className="text-brand hover:underline disabled:pointer-events-none disabled:opacity-50"
              disabled={unbinding}
              onClick={handleUnbindWechat}
            >
              {unbinding ? copy.unbinding : copy.unbind}
            </button>
          )}
        </span>
      </div>

      <div className="mt-10 overflow-hidden rounded-xl border border-destructive/15 bg-destructive/[0.02] px-4">
        <div className="flex min-h-16 items-center justify-between gap-6 border-b border-destructive/10 py-4">
          <span className="text-sm text-muted-foreground">{copy.logoutAll}</span>
          <Button
            variant="outline"
            size="sm"
            className={dangerPillClass}
            disabled={loggingOutAll}
            onClick={handleLogoutAll}
          >
            {loggingOutAll ? copy.loggingOut : copy.logout}
          </Button>
        </div>
        <div className="flex min-h-16 items-center justify-between gap-6 py-4">
          <span className="text-sm text-muted-foreground">{copy.cancelAccount}</span>
          <Button
            variant="outline"
            size="sm"
            className={dangerPillClass}
            onClick={() => setCancelOpen(true)}
          >
            {copy.cancelAccount}
          </Button>
        </div>
      </div>

      <ChangeUsernameDialog
        open={changeUsernameOpen}
        onOpenChange={setChangeUsernameOpen}
        currentUsername={user?.nickname || ""}
        onSuccess={(nickname) => user && setUser({ ...user, nickname })}
      />

      <ChangePhoneDialog
        open={changePhoneOpen}
        onOpenChange={setChangePhoneOpen}
        onSuccess={(phone) => user && setUser({ ...user, phone })}
      />

      <Dialog open={cancelOpen} onOpenChange={setCancelOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{copy.confirmCancelTitle}</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            {copy.confirmCancelBody}
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelOpen(false)}>
              {copy.cancel}
            </Button>
            <Button
              variant="destructive"
              disabled={cancelling}
              onClick={handleCancelAccount}
            >
              {cancelling ? copy.cancelling : copy.confirmCancel}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function ChangeUsernameDialog({
  open,
  onOpenChange,
  currentUsername,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentUsername: string;
  onSuccess: (username: string) => void;
}) {
  const [username, setUsername] = useState(currentUsername);
  const [submitting, setSubmitting] = useState(false);
  const locale = useLocaleStore((s) => s.locale);
  const copy = SETTINGS_COPY[locale];
  const normalizedUsername = username.trim();
  const canSubmit =
    normalizedUsername.length > 0 &&
    normalizedUsername.length <= 64 &&
    normalizedUsername !== currentUsername &&
    !submitting;

  const handleOpenChange = (nextOpen: boolean) => {
    setUsername(currentUsername);
    onOpenChange(nextOpen);
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    try {
      await userApi.update({ nickname: normalizedUsername });
      onSuccess(normalizedUsername);
      toast.success(copy.usernameChanged);
      onOpenChange(false);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : copy.usernameChangeFailed);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{copy.changeUsername}</DialogTitle>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="new-username">{copy.newUsername}</Label>
          <Input
            id="new-username"
            maxLength={64}
            value={username}
            placeholder={copy.newUsernamePlaceholder}
            autoComplete="nickname"
            onChange={(event) => setUsername(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") void handleSubmit();
            }}
          />
          <p className="text-xs text-muted-foreground">{copy.usernameHint}</p>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => handleOpenChange(false)}>
            {copy.cancel}
          </Button>
          <Button disabled={!canSubmit} onClick={handleSubmit}>
            {submitting ? copy.submitting : copy.confirmChange}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ChangePhoneDialog({
  open,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: (phone: string) => void;
}) {
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [sending, setSending] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const { remaining, active, start } = useCountdown(60);
  const locale = useLocaleStore((s) => s.locale);
  const copy = SETTINGS_COPY[locale];

  const phoneOk = isValidPhone(phone);
  const canSubmit = phoneOk && isValidCode(code) && !submitting;

  const reset = () => {
    setPhone("");
    setCode("");
  };

  const handleSend = async () => {
    if (!phoneOk || active || sending) return;
    setSending(true);
    try {
      await authApi.sendSmsCode(phone.trim());
      start();
      toast.success(copy.codeSent);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : copy.sendCodeFailed);
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    try {
      await accountMock.changePhone(phone.trim(), code.trim());
      toast.success(copy.phoneChanged);
      onSuccess(phone.trim());
      onOpenChange(false);
      reset();
    } catch {
      toast.error(copy.changeFailed);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        onOpenChange(o);
        if (!o) reset();
      }}
    >
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
            <DialogTitle>{copy.changePhone}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="new-phone">{copy.newPhone}</Label>
              <Input
                id="new-phone"
                inputMode="numeric"
                maxLength={11}
                placeholder={copy.newPhonePlaceholder}
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
            />
          </div>
          <div className="space-y-2">
              <Label htmlFor="new-phone-code">{copy.code}</Label>
              <div className="flex gap-2">
                <Input
                  id="new-phone-code"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder={copy.codePlaceholder}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
              />
              <Button
                type="button"
                variant="outline"
                className="shrink-0"
                disabled={!phoneOk || active || sending}
                onClick={handleSend}
              >
                {active ? `${remaining}s` : sending ? copy.sending : copy.sendCode}
              </Button>
            </div>
          </div>
        </div>
        <DialogFooter>
            <Button variant="outline" onClick={() => onOpenChange(false)}>
              {copy.cancel}
            </Button>
            <Button disabled={!canSubmit} onClick={handleSubmit}>
              {submitting ? copy.submitting : copy.confirmChange}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
