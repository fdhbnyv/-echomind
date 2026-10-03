-- ============================================================
-- EchoMind 云端数据库 Schema（Supabase / Postgres）
--
-- 使用方法：
--   1. 在 https://supabase.com 创建项目（免费档即可）
--   2. 打开 Dashboard → SQL Editor → 粘贴本文件全部内容 → Run
--   3. 打开 Dashboard → Project Settings → API，复制：
--        - Project URL   （形如 https://xxxx.supabase.co）
--        - anon public key
--      填入 EchoMind「设置 → 云端同步」
--
-- 设计说明：
--   * 主键使用客户端生成的 uuid（文本），多设备离线写入不冲突
--   * 所有时间戳使用 bigint 毫秒（与客户端 System.currentTimeMillis 对齐）
--   * 数组字段使用 jsonb，与客户端 StructuredNote/Memory 的 JSON 列表一一对应
--   * updated_at 驱动增量拉取（last-write-wins 合并）
--
-- 安全说明（V1 个人版）：
--   * RLS 已开启，策略对 anon 角色完全放行 —— anon key 即访问凭据，
--     等同于 Notion token，请勿将 anon key 公开分享
--   * V2（用户系统上线后）应接入 Supabase Auth，将策略改为
--     `auth.uid() = user_id`，并为两张表增加 user_id 列
-- ============================================================

-- ── 笔记表（对应 Room: notes / NoteEntity） ──
create table if not exists public.notes (
    uuid              text primary key,
    device_id         text not null default '',
    template_type     text not null,
    title             text not null,
    date              text not null,
    summary           text not null default '',
    accomplishments   jsonb not null default '[]',
    challenges        jsonb not null default '[]',
    action_items      jsonb not null default '[]',
    key_points        jsonb not null default '[]',
    ideas             jsonb not null default '[]',
    schedule          jsonb not null default '[]',
    mood              text,
    tags              jsonb not null default '[]',
    raw_transcription text not null default '',
    is_voice          boolean not null default false,
    created_at        bigint not null,
    updated_at        bigint not null
);

-- ── 记忆表（对应 Room: memories / MemoryEntity） ──
create table if not exists public.memories (
    uuid             text primary key,
    device_id        text not null default '',
    content          text not null,
    category         text not null,
    type             text not null,
    tags             jsonb not null default '[]',
    importance       integer not null default 3,
    source           text not null default 'manual',
    is_active        boolean not null default true,
    created_at       bigint not null,
    last_accessed_at bigint not null,
    access_count     integer not null default 0,
    updated_at       bigint not null
);

-- 增量拉取按 updated_at 扫描
create index if not exists notes_updated_at_idx on public.notes (updated_at);
create index if not exists memories_updated_at_idx on public.memories (updated_at);

-- ── 行级安全（V1：anon key 即凭据，见文件头说明） ──
alter table public.notes enable row level security;
alter table public.memories enable row level security;

drop policy if exists "anon_full_access_notes" on public.notes;
create policy "anon_full_access_notes" on public.notes
    for all to anon using (true) with check (true);

drop policy if exists "anon_full_access_memories" on public.memories;
create policy "anon_full_access_memories" on public.memories
    for all to anon using (true) with check (true);
