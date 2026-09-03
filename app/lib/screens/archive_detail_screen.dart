import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:file_picker/file_picker.dart';

import '../api/api_client.dart';
import '../models.dart';
import 'chat_screen.dart';

class ArchiveDetailScreen extends StatefulWidget {
  final ApiClient api;
  final String token;
  final int userId;
  final LovedOne lovedOne;

  const ArchiveDetailScreen({
    super.key,
    required this.api,
    required this.token,
    required this.userId,
    required this.lovedOne,
  });

  @override
  State<ArchiveDetailScreen> createState() => _ArchiveDetailScreenState();
}

class _ArchiveDetailScreenState extends State<ArchiveDetailScreen> {
  late final TextEditingController _name;
  late final TextEditingController _birthDate;
  late final TextEditingController _deathDate;
  late final TextEditingController _birthPlace;
  late final TextEditingController _bio;
  late LovedOne _current;
  bool _editing = false;
  bool _saving = false;
  bool _uploading = false;
  String? _error;
  String? _mediaError;
  List<MediaItem> _media = [];
  List<ConsentRecord> _consents = [];
  List<FamilyMemberInfo> _members = [];
  bool _consentsLoading = true;
  String? _consentsError;

  @override
  void initState() {
    super.initState();
    _current = widget.lovedOne;
    _name = TextEditingController(text: _current.name);
    _birthDate = TextEditingController(text: _current.birthDate ?? '');
    _deathDate = TextEditingController(text: _current.deathDate ?? '');
    _birthPlace = TextEditingController(text: _current.birthPlace ?? '');
    _bio = TextEditingController(text: _current.bio ?? '');
    _loadMedia();
    _loadConsents();
  }

  @override
  void dispose() {
    _name.dispose();
    _birthDate.dispose();
    _deathDate.dispose();
    _birthPlace.dispose();
    _bio.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final name = _name.text.trim();
    if (name.isEmpty) {
      setState(() => _error = '姓名不能为空');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final updated = await widget.api.updateLovedOne(
        widget.token,
        _current.id,
        name: name,
        birthDate: _birthDate.text.trim(),
        deathDate: _deathDate.text.trim(),
        birthPlace: _birthPlace.text.trim(),
        bio: _bio.text.trim(),
      );
      if (!mounted) return;
      setState(() {
        _current = updated;
        _editing = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('资料已保存')));
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = '保存失败，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _openChat() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ChatScreen(api: widget.api, token: widget.token, lovedOne: _current),
      ),
    );
  }

  Future<void> _loadMedia() async {
    try {
      final media = await widget.api.listMedia(widget.token, _current.id);
      if (!mounted) return;
      setState(() => _media = media);
    } catch (_) {
      if (mounted) setState(() => _mediaError = '素材加载失败，请稍后重试');
    }
  }

  Future<void> _loadConsents() async {
    try {
      final results = await Future.wait([
        widget.api.listConsents(widget.token, _current.id),
        widget.api.listFamilyMembers(widget.token, _current.familyId),
      ]);
      if (!mounted) return;
      setState(() {
        _consents = results[0] as List<ConsentRecord>;
        _members = results[1] as List<FamilyMemberInfo>;
        _consentsLoading = false;
        _consentsError = null;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _consentsLoading = false;
          _consentsError = '知情同意记录加载失败';
        });
      }
    }
  }

  Future<void> _pickAndUpload(String mediaType) async {
    try {
      final file = await FilePicker.pickFile(
        type: mediaType == 'PHOTO' ? FileType.image : FileType.audio,
      );
      if (file == null) return;
      final bytes = await file.readAsBytes();
      setState(() => _uploading = true);
      final item = await widget.api.uploadMedia(
        widget.token,
        _current.id,
        mediaType,
        file.name.isEmpty ? 'upload' : file.name,
        bytes,
      );
      if (!mounted) return;
      setState(() => _media.insert(0, item));
      _showSnack('上传成功');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('上传失败，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${_current.name} 的档案'),
        actions: [
          IconButton(
            tooltip: '邀请家人',
            icon: const Icon(Icons.person_add_alt_outlined),
            onPressed: _openInviteDialog,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('基本信息', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          if (_editing) ...[
            TextField(
              controller: _name,
              decoration: const InputDecoration(labelText: '姓名', border: OutlineInputBorder()),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _birthDate,
              decoration: const InputDecoration(
                labelText: '出生日期（如 1940-01-01）',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _deathDate,
              decoration: const InputDecoration(
                labelText: '逝世日期（如 2020-05-01）',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _birthPlace,
              decoration: const InputDecoration(labelText: '籍贯', border: OutlineInputBorder()),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _bio,
              maxLines: 5,
              decoration: const InputDecoration(labelText: '生平简介', border: OutlineInputBorder()),
            ),
          ] else ...[
            _InfoRow(label: '姓名', value: _current.name),
            _InfoRow(label: '出生', value: _current.birthDate ?? '未填写'),
            _InfoRow(label: '逝世', value: _current.deathDate ?? '未填写'),
            _InfoRow(label: '籍贯', value: _current.birthPlace ?? '未填写'),
            _InfoRow(label: '简介', value: _current.bio ?? '未填写'),
          ],
          if (_error != null) ...[
            const SizedBox(height: 8),
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: _openChat,
                  icon: const Icon(Icons.chat_bubble_outline),
                  label: const Text('去聊天'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _editing
                    ? FilledButton.tonalIcon(
                        onPressed: _saving ? null : _save,
                        icon: const Icon(Icons.save_outlined),
                        label: Text(_saving ? '保存中…' : '保存修改'),
                      )
                    : OutlinedButton.icon(
                        onPressed: () => setState(() => _editing = true),
                        icon: const Icon(Icons.edit_outlined),
                        label: const Text('编辑资料'),
                      ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (!_current.isEffectivelyDeceased && _current.userId == widget.userId) ...[
            Text('AI 讲述', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Card(
              child: ListTile(
                leading: Icon(
                  _current.aiPersonaEnabled ? Icons.record_voice_over : Icons.voice_over_off,
                  color: _current.aiPersonaEnabled ? Colors.green : Colors.grey,
                ),
                title: Text(_current.aiPersonaEnabled ? 'AI 讲述已开启' : 'AI 讲述未开启'),
                subtitle: Text(_current.aiPersonaEnabled
                    ? '故事问答可用，可随时关闭'
                    : '仅本人可开启；开启后家人可基于你的讲述向你提问'),
                trailing: _current.aiPersonaEnabled
                    ? OutlinedButton(
                        onPressed: () => _toggleAi(false),
                        child: const Text('关闭'),
                      )
                    : FilledButton(
                        onPressed: () => _toggleAi(true),
                        child: const Text('开启'),
                      ),
              ),
            ),
          ],
          if (_current.isEffectivelyDeceased) ...[
            const SizedBox(height: 16),
            Text('故人档案', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 4),
            const Text('故事问答需完成知情同意后可用。'),
          ],
          const SizedBox(height: 24),
          Text('素材与记录', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: FilledButton.tonalIcon(
                  onPressed: _uploading ? null : () => _pickAndUpload('PHOTO'),
                  icon: const Icon(Icons.add_photo_alternate_outlined),
                  label: const Text('添加照片'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _uploading ? null : () => _pickAndUpload('AUDIO'),
                  icon: const Icon(Icons.add_box_outlined),
                  label: const Text('添加录音'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (_uploading) const LinearProgressIndicator(),
          if (_mediaError != null) ...[
            const SizedBox(height: 8),
            Text(_mediaError!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
          const SizedBox(height: 8),
          if (_media.isEmpty && _mediaError == null)
            const Text('还没有照片或录音，添加后这里会展示。')
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _media.map(_mediaCard).toList(),
            ),
          const SizedBox(height: 16),
          Row(
            children: [
              Text('知情同意', style: Theme.of(context).textTheme.titleMedium),
              const Spacer(),
              TextButton.icon(
                onPressed: _openConsentDialog,
                icon: const Icon(Icons.add),
                label: const Text('提交授权'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (_consentsLoading)
            const LinearProgressIndicator()
          else if (_consentsError != null)
            Text(_consentsError!, style: TextStyle(color: Theme.of(context).colorScheme.error))
          else if (_consents.isEmpty)
            const Text('尚未提交知情同意记录，创建档案时由家人完成确认。')
          else
            ..._consents.map(_consentTile),
        ],
      ),
    );
  }

  Future<void> _openConsentDialog() async {
    if (_members.isEmpty) {
      _showSnack('暂无可选确认人，请先邀请家族成员');
      return;
    }
    final submitted = await showDialog<bool>(
      context: context,
      builder: (_) => _ConsentDialog(
        api: widget.api,
        token: widget.token,
        lovedOneId: _current.id,
        members: _members,
      ),
    );
    if (submitted == true && mounted) {
      await _loadConsents();
      _showSnack('授权记录已提交');
    }
  }

  Future<void> _openInviteDialog() async {
    await showDialog<void>(
      context: context,
      builder: (_) => _InviteDialog(
        api: widget.api,
        token: widget.token,
        lovedOneId: _current.id,
      ),
    );
  }

  Future<void> _toggleAi(bool enable) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(enable ? '开启 AI 讲述？' : '关闭 AI 讲述？'),
        content: Text(enable
            ? '仅本人可开启。开启后，家人可基于你的讲述/口述档案向你提问，AI 回答会带标识且仅使用你授权的内容。'
            : '关闭后故事问答立即不可用。'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(enable ? '开启' : '关闭'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      final enabled = enable
          ? await widget.api.enableAi(widget.token, _current.id)
          : !await widget.api.disableAi(widget.token, _current.id);
      if (!mounted) return;
      setState(() => _current = _current.copyWith(aiPersonaEnabled: enabled));
      _showSnack(enable ? 'AI 讲述已开启' : 'AI 讲述已关闭');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('操作失败，请稍后重试');
    }
  }

  Widget _consentTile(ConsentRecord record) {
    final names = record.consentorIds
        .map((id) => _members
                .where((m) => m.userId == id)
                .map((m) => m.name ?? m.phone ?? '#$id')
                .firstOrNull ??
            '#$id')
        .join('、');
    final typeLabel = record.consentType == 'PRE_AUTHORIZED' ? '故人生前预授权' : '两名近亲共同确认';
    final valid = record.status == 'VALID';
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      child: ListTile(
        leading: Icon(
          valid ? Icons.verified_user : Icons.pending_outlined,
          color: valid ? Colors.green : Colors.orange,
        ),
        title: Text(typeLabel),
        subtitle: Text('确认人：$names\n签署时间：${_formatTime(record.signedAt)}'),
        trailing: Text(
          valid ? '有效' : record.status,
          style: TextStyle(
            color: valid ? Colors.green : Colors.orange,
            fontSize: 12,
          ),
        ),
      ),
    );
  }

  String _formatTime(String? iso) {
    if (iso == null || iso.isEmpty) return '-';
    final time = DateTime.tryParse(iso)?.toLocal();
    if (time == null) return iso;
    String two(int v) => v.toString().padLeft(2, '0');
    return '${time.year}-${two(time.month)}-${two(time.day)} ${two(time.hour)}:${two(time.minute)}';
  }

  Widget _mediaCard(MediaItem item) {
    final sizeLabel = item.sizeBytes == null
        ? ''
        : ' · ${(item.sizeBytes! / 1024).toStringAsFixed(1)}KB';
    return GestureDetector(
      onTap: item.mediaType == 'PHOTO' && item.url != null
          ? () => _openGallery(item)
          : null,
      child: Tooltip(
        message: item.objectKey ?? item.mediaType,
        child: Container(
          width: 110,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(8),
          ),
          clipBehavior: Clip.antiAlias,
          child: Stack(
            children: [
              Column(
                children: [
                  SizedBox(
                    height: 80,
                    width: double.infinity,
                    child: item.mediaType == 'PHOTO' && item.url != null
                        ? Image.network(
                            item.url!,
                            fit: BoxFit.cover,
                            errorBuilder: (_, _, _) =>
                                const Icon(Icons.broken_image_outlined, size: 40),
                          )
                        : const Icon(Icons.graphic_eq, size: 40),
                  ),
                  Padding(
                    padding: const EdgeInsets.all(4),
                    child: Text(
                      '${item.mediaType == 'PHOTO' ? '照片' : '录音'}$sizeLabel',
                      style: const TextStyle(fontSize: 11),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Positioned(
                top: 0,
                right: 0,
                child: IconButton(
                  tooltip: '删除',
                  iconSize: 16,
                  visualDensity: VisualDensity.compact,
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints.tightFor(width: 26, height: 26),
                  style: IconButton.styleFrom(
                    backgroundColor: Colors.black54,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: () => _deleteMedia(item),
                  icon: const Icon(Icons.close),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _openGallery(MediaItem tapped) async {
    final photos = _media.where((m) => m.mediaType == 'PHOTO').toList();
    if (photos.isEmpty) return;
    final index = photos.indexWhere((m) => m.id == tapped.id);
    if (index < 0) return;
    await showDialog<void>(
      context: context,
      builder: (_) => _PhotoGalleryDialog(
        api: widget.api,
        token: widget.token,
        lovedOneId: _current.id,
        photos: photos,
        initialIndex: index,
      ),
    );
  }

  Future<void> _deleteMedia(MediaItem item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('删除素材？'),
        content: const Text('删除后不可恢复。'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await widget.api.deleteMedia(widget.token, _current.id, item.id);
      if (!mounted) return;
      setState(() => _media.removeWhere((m) => m.id == item.id));
      _showSnack('已删除');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('删除失败，请重试');
    }
  }
}

class _InviteDialog extends StatefulWidget {
  final ApiClient api;
  final String token;
  final int lovedOneId;

  const _InviteDialog({
    required this.api,
    required this.token,
    required this.lovedOneId,
  });

  @override
  State<_InviteDialog> createState() => _InviteDialogState();
}

class _InviteDialogState extends State<_InviteDialog> {
  final TextEditingController _display = TextEditingController();
  final FocusNode _codeFocus = FocusNode();
  String _role = 'VIEWER';
  bool _generating = false;
  bool _copied = false;
  String? _error;
  String? _copyError;
  InviteKeyInfo? _key;

  Future<void> _generate() async {
    setState(() {
      _generating = true;
      _error = null;
    });
    try {
      final key = await widget.api.generateInviteKey(
        widget.token,
        widget.lovedOneId,
        role: _role,
      );
      if (!mounted) return;
      setState(() {
        _key = key;
        _display.text = key.code;
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '生成失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _generating = false);
    }
  }

  @override
  void dispose() {
    _display.dispose();
    _codeFocus.dispose();
    super.dispose();
  }

  Future<void> _copy() async {
    if (_key == null) return;
    try {
      await Clipboard.setData(ClipboardData(text: _key!.code));
      if (mounted) {
        setState(() {
          _copied = true;
          _copyError = null;
        });
      }
    } catch (_) {
      if (mounted) {
        _codeFocus.requestFocus();
        _display.selection =
            TextSelection(baseOffset: 0, extentOffset: _display.text.length);
        setState(() => _copyError = '浏览器限制自动复制：已为你全选邀请码，请按 Ctrl+C（Mac：Cmd+C）。');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(_key == null ? '邀请家人' : '邀请码已生成'),
      content: _key == null ? _buildPickRole() : _buildShowCode(),
      actions: [
        TextButton(
          onPressed: _generating ? null : () => Navigator.pop(context),
          child: const Text('关闭'),
        ),
        if (_key == null)
          FilledButton(
            onPressed: _generating ? null : _generate,
            child: Text(_generating ? '生成中…' : '生成邀请码'),
          ),
      ],
    );
  }

  Widget _buildPickRole() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('对方加入后获得的权限'),
        const SizedBox(height: 8),
        SegmentedButton<String>(
          segments: const [
            ButtonSegment(value: 'VIEWER', label: Text('只读')),
            ButtonSegment(value: 'EDITOR', label: Text('共建')),
          ],
          selected: {_role},
          onSelectionChanged: (selection) => setState(() => _role = selection.first),
        ),
        const SizedBox(height: 12),
        const Text('邀请码为 16 位，72 小时内有效、仅可使用一次，请通过可信渠道发送给家人。'),
        if (_error != null) ...[
          const SizedBox(height: 8),
          Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
        ],
      ],
    );
  }

  Widget _buildShowCode() {
    final key = _key!;
    final expires = DateTime.tryParse(key.expiresAt ?? '')?.toLocal();
    final expiresText = expires == null
        ? ''
        : '有效期至 ${expires.year}-${expires.month.toString().padLeft(2, '0')}-${expires.day.toString().padLeft(2, '0')} ${expires.hour.toString().padLeft(2, '0')}:${expires.minute.toString().padLeft(2, '0')}';
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          controller: _display,
          focusNode: _codeFocus,
          readOnly: true,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, letterSpacing: 1.2),
          onTap: () => _display.selection =
              TextSelection(baseOffset: 0, extentOffset: _display.text.length),
        ),
        const SizedBox(height: 12),
        FilledButton.tonalIcon(
          onPressed: _copy,
          icon: Icon(_copied ? Icons.check : Icons.copy),
          label: Text(_copied ? '已复制' : '复制邀请码'),
        ),
        if (_copyError != null) ...[
          const SizedBox(height: 8),
          Text(_copyError!, style: const TextStyle(fontSize: 12, color: Colors.orangeAccent)),
        ],
        const SizedBox(height: 8),
        Text('单次使用 · ${key.role == 'EDITOR' ? '共建' : '只读'}权限 · $expiresText',
            style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}

class _ConsentDialog extends StatefulWidget {
  final ApiClient api;
  final String token;
  final int lovedOneId;
  final List<FamilyMemberInfo> members;

  const _ConsentDialog({
    required this.api,
    required this.token,
    required this.lovedOneId,
    required this.members,
  });

  @override
  State<_ConsentDialog> createState() => _ConsentDialogState();
}

class _ConsentDialogState extends State<_ConsentDialog> {
  String _type = 'TWO_RELATIVES';
  final Set<int> _selected = {};
  bool _submitting = false;
  String? _error;

  int get _required => _type == 'PRE_AUTHORIZED' ? 1 : 2;

  void _submit() async {
    if (_selected.length < _required) {
      setState(() => _error = _type == 'PRE_AUTHORIZED'
          ? '生前预授权至少选择 1 位确认人'
          : '两名近亲共同确认至少选择 2 位确认人');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await widget.api.createConsent(
        widget.token,
        widget.lovedOneId,
        _type,
        _selected.toList(),
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '提交失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('提交授权记录'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('授权类型'),
            const SizedBox(height: 8),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'PRE_AUTHORIZED', label: Text('生前预授权')),
                ButtonSegment(value: 'TWO_RELATIVES', label: Text('两名近亲确认')),
              ],
              selected: {_type},
              onSelectionChanged: (selection) {
                setState(() => _type = selection.first);
              },
            ),
            const SizedBox(height: 16),
            Text('确认人（至少 $_required 位）'),
            const SizedBox(height: 4),
            ...widget.members.map(
              (m) => CheckboxListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                value: _selected.contains(m.userId),
                title: Text(m.name ?? m.phone ?? '#${m.userId}'),
                subtitle: m.phone == null ? null : Text(m.phone!),
                onChanged: (checked) {
                  setState(() {
                    if (checked == true) {
                      _selected.add(m.userId);
                    } else {
                      _selected.remove(m.userId);
                    }
                  });
                },
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _submitting ? null : () => Navigator.pop(context, false),
          child: const Text('取消'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: Text(_submitting ? '提交中…' : '提交'),
        ),
      ],
    );
  }
}

class _PhotoGalleryDialog extends StatefulWidget {
  final ApiClient api;
  final String token;
  final int lovedOneId;
  final List<MediaItem> photos;
  final int initialIndex;

  const _PhotoGalleryDialog({
    required this.api,
    required this.token,
    required this.lovedOneId,
    required this.photos,
    required this.initialIndex,
  });

  @override
  State<_PhotoGalleryDialog> createState() => _PhotoGalleryDialogState();
}

class _PhotoGalleryDialogState extends State<_PhotoGalleryDialog> {
  final Map<int, String> _urls = {};
  late int _index;
  String? _error;

  int get _count => widget.photos.length;

  MediaItem get _current => widget.photos[_index];

  @override
  void initState() {
    super.initState();
    _index = widget.initialIndex;
    _loadUrl();
  }

  Future<void> _loadUrl() async {
    final id = _current.id;
    if (_urls.containsKey(id)) {
      setState(() => _error = null);
      return;
    }
    setState(() => _error = null);
    try {
      final url = await widget.api.getMediaUrl(widget.token, widget.lovedOneId, id);
      if (!mounted) return;
      setState(() => _urls[id] = url);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '获取图片地址失败，请重试');
    }
  }

  void _move(int delta) {
    if (_count <= 1) return;
    final next = (_index + delta + _count) % _count;
    if (next == _index) return;
    setState(() {
      _index = next;
      _error = null;
    });
    _loadUrl();
  }

  void _close() {
    Navigator.of(context).pop();
  }

  KeyEventResult _handleKey(FocusNode node, KeyEvent event) {
    if (event is! KeyDownEvent) return KeyEventResult.ignored;
    if (event.logicalKey == LogicalKeyboardKey.arrowRight) {
      _move(1);
      return KeyEventResult.handled;
    }
    if (event.logicalKey == LogicalKeyboardKey.arrowLeft) {
      _move(-1);
      return KeyEventResult.handled;
    }
    if (event.logicalKey == LogicalKeyboardKey.escape) {
      _close();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final url = _urls[_current.id];
    return Dialog(
      insetPadding: EdgeInsets.zero,
      backgroundColor: Colors.black,
      child: Focus(
        autofocus: true,
        onKeyEvent: _handleKey,
        child: Container(
          width: size.width,
          height: size.height,
          color: Colors.black,
          child: Stack(
            children: [
              Center(
                child: _error != null && url == null
                    ? _buildError()
                    : url == null
                        ? const CircularProgressIndicator(color: Colors.white)
                        : InteractiveViewer(
                            minScale: 0.8,
                            maxScale: 5,
                            child: Image.network(
                              url,
                              fit: BoxFit.contain,
                              width: size.width,
                              height: size.height,
                              errorBuilder: (_, _, _) => _buildError(),
                            ),
                          ),
              ),
              if (_count > 1) ...[
                Positioned(
                  left: 8,
                  top: 0,
                  bottom: 0,
                  child: Center(
                    child: IconButton(
                      tooltip: '上一张（←）',
                      iconSize: 40,
                      color: Colors.white,
                      onPressed: () => _move(-1),
                      icon: const Icon(Icons.chevron_left),
                    ),
                  ),
                ),
                Positioned(
                  right: 8,
                  top: 0,
                  bottom: 0,
                  child: Center(
                    child: IconButton(
                      tooltip: '下一张（→）',
                      iconSize: 40,
                      color: Colors.white,
                      onPressed: () => _move(1),
                      icon: const Icon(Icons.chevron_right),
                    ),
                  ),
                ),
              ],
              Positioned(
                top: 8,
                right: 8,
                child: IconButton(
                  tooltip: '关闭',
                  color: Colors.white,
                  onPressed: _close,
                  icon: const Icon(Icons.close),
                ),
              ),
              Positioned(
                bottom: 16,
                left: 0,
                right: 0,
                child: Text(
                  '${_index + 1} / $_count',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white70),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildError() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Icon(Icons.broken_image_outlined, color: Colors.white70, size: 48),
        const SizedBox(height: 8),
        Text(_error ?? '图片加载失败', style: const TextStyle(color: Colors.white70)),
        TextButton(
          onPressed: _loadUrl,
          child: const Text('重试', style: TextStyle(color: Colors.white)),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 64,
            child: Text(label, style: const TextStyle(color: Colors.grey)),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}
