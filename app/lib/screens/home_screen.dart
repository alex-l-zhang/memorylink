import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../auth/session_store.dart';
import '../models.dart';
import '../relation_options.dart';
import 'archive_detail_screen.dart';
import 'login_screen.dart';
import 'my_oral_screen.dart';
import 'my_assets_screen.dart';

class HomeScreen extends StatefulWidget {
  final ApiClient api;
  final String token;
  final int userId;

  const HomeScreen({super.key, required this.api, required this.token, required this.userId});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<LovedOne> _lovedOnes = [];
  UserProfile? _profile;
  bool _loading = true;
  String? _error;
  bool _profileLoaded = false;

  @override
  void initState() {
    super.initState();
    _reload();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final profile = await widget.api.me(widget.token);
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _profileLoaded = true;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.code == 1001 || e.code == 1002) {
        await _gotoLogin();
        return;
      }
      if (mounted) setState(() => _profileLoaded = true);
    } catch (_) {
      if (mounted) setState(() => _profileLoaded = true);
    }
  }

  Future<void> _openProfileDialog() async {
    final updated = await showDialog<UserProfile>(
      context: context,
      builder: (_) => _ProfileDialog(api: widget.api, token: widget.token, profile: _profile),
    );
    if (updated != null && mounted) {
      setState(() => _profile = updated);
      _showSnack('资料已保存');
    }
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await widget.api.listLovedOnes(widget.token);
      if (!mounted) return;
      setState(() => _lovedOnes = list);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.code == 1001 || e.code == 1002) {
        await _gotoLogin();
        return;
      }
      setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '加载失败，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _create() async {
    final controller = TextEditingController();
    final name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('创建记忆档案'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(labelText: '故人姓名'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('创建'),
          ),
        ],
      ),
    );
    if (name == null || name.isEmpty) return;
    try {
      await widget.api.createLovedOne(widget.token, name);
      await _reload();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('记忆档案'),
        actions: [
          IconButton(
            tooltip: '我的素材与记录',
            icon: const Icon(Icons.photo_library_outlined),
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => MyAssetsScreen(api: widget.api, token: widget.token),
              ),
            ),
          ),
          IconButton(
            tooltip: '我的讲述',
            icon: const Icon(Icons.record_voice_over_outlined),
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => MyOralScreen(api: widget.api, token: widget.token),
              ),
            ),
          ),
          IconButton(
            tooltip: '我的资料',
            icon: const Icon(Icons.person_outline),
            onPressed: _openProfileDialog,
          ),
          IconButton(
            tooltip: '用邀请码加入纪念馆',
            icon: const Icon(Icons.group_add_outlined),
            onPressed: _openClaimDialog,
          ),
          IconButton(
            tooltip: '退出登录',
            icon: const Icon(Icons.logout),
            onPressed: _logout,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _create,
        tooltip: '创建记忆档案',
        child: const Icon(Icons.add),
      ),
      body: RefreshIndicator(
        onRefresh: _reload,
        child: Column(
          children: [
            if (_profileLoaded && _profile?.birthDate == null) _buildProfileBanner(),
            Expanded(child: _buildBody()),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileBanner() {
    return Card(
      color: Theme.of(context).colorScheme.primaryContainer,
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      child: ListTile(
        leading: const Icon(Icons.badge_outlined),
        title: const Text('完善出生日期后可使用故事问答'),
        trailing: TextButton(
          onPressed: _openProfileDialog,
          child: const Text('去完善'),
        ),
      ),
    );
  }

  Future<void> _openClaimDialog() async {
    final claimed = await showDialog<bool>(
      context: context,
      builder: (_) => _ClaimDialog(api: widget.api, token: widget.token),
    );
    if (claimed == true && mounted) {
      await _reload();
      _showSnack('已加入纪念馆');
    }
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _gotoLogin() async {
    await SessionStore.clear();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => LoginScreen(api: widget.api)),
    );
  }

  Future<void> _logout() => _gotoLogin();

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!),
            const SizedBox(height: 12),
            FilledButton(onPressed: _reload, child: const Text('重试')),
          ],
        ),
      );
    }
    if (_lovedOnes.isEmpty) {
      return ListView(
        children: const [
          Padding(
            padding: EdgeInsets.all(32),
            child: Center(child: Text('还没有记忆档案，点击右下角 + 创建')),
          ),
        ],
      );
    }
    return ListView.builder(
      itemCount: _lovedOnes.length,
      itemBuilder: (context, index) {
        final item = _lovedOnes[index];
        return ListTile(
          leading: const CircleAvatar(child: Icon(Icons.person)),
          title: Text(item.name),
          subtitle: Text([
            item.isEffectivelyDeceased ? '故人' : '在世',
            if (item.birthDate != null) '生 ${item.birthDate}',
            if (item.deathDate != null) '卒 ${item.deathDate}',
            if (item.birthPlace != null) item.birthPlace!,
          ].join(' · ')),
          trailing: const Icon(Icons.chevron_right),
          onTap: () async {
            await Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) =>
                    ArchiveDetailScreen(
                      api: widget.api,
                      token: widget.token,
                      userId: widget.userId,
                      lovedOne: item,
                    ),
              ),
            );
            if (mounted) _reload();
          },
        );
      },
    );
  }
}

class _ProfileDialog extends StatefulWidget {
  final ApiClient api;
  final String token;
  final UserProfile? profile;

  const _ProfileDialog({required this.api, required this.token, this.profile});

  @override
  State<_ProfileDialog> createState() => _ProfileDialogState();
}

class _ProfileDialogState extends State<_ProfileDialog> {
  late final TextEditingController _name;
  late final TextEditingController _birthDate;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _name = TextEditingController(text: widget.profile?.name ?? '');
    _birthDate = TextEditingController(text: widget.profile?.birthDate ?? '');
  }

  @override
  void dispose() {
    _name.dispose();
    _birthDate.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final birthText = _birthDate.text.trim();
    if (birthText.isNotEmpty && !RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(birthText)) {
      setState(() => _error = '出生日期格式应为 YYYY-MM-DD');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final updated = await widget.api.updateProfile(
        widget.token,
        name: _name.text.trim(),
        birthDate: birthText.isEmpty ? null : birthText,
      );
      if (!mounted) return;
      Navigator.of(context).pop(updated);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '保存失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('我的资料'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _name,
            decoration: const InputDecoration(labelText: '姓名', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _birthDate,
            decoration: const InputDecoration(
              labelText: '出生日期（YYYY-MM-DD）',
              border: OutlineInputBorder(),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 8),
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
        ],
      ),
      actions: [
        TextButton(onPressed: _saving ? null : () => Navigator.pop(context), child: const Text('取消')),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: Text(_saving ? '保存中…' : '保存'),
        ),
      ],
    );
  }
}

class _ClaimDialog extends StatefulWidget {
  final ApiClient api;
  final String token;

  const _ClaimDialog({required this.api, required this.token});

  @override
  State<_ClaimDialog> createState() => _ClaimDialogState();
}

class _ClaimDialogState extends State<_ClaimDialog> {
  final _code = TextEditingController();
  String _relation = 'CHILD';
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await widget.api.claimInvite(
        widget.token,
        _code.text.trim(),
        _relation,
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '加入失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('用邀请码加入纪念馆'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _code,
            autofocus: true,
            decoration: const InputDecoration(
              labelText: '邀请码（如 ABCD-EFGH-JKLM-NPQR）',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _relation,
            decoration: const InputDecoration(labelText: '你与故人的关系', border: OutlineInputBorder()),
            items: relationOptions
                .map((o) => DropdownMenuItem(value: o.code, child: Text(o.label)))
                .toList(),
            onChanged: (value) {
              if (value != null) setState(() => _relation = value);
            },
          ),
          if (_error != null) ...[
            const SizedBox(height: 8),
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: _submitting ? null : () => Navigator.pop(context, false),
          child: const Text('取消'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: Text(_submitting ? '加入中…' : '加入'),
        ),
      ],
    );
  }
}
