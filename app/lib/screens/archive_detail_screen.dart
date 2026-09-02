import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';

import '../api/api_client.dart';
import '../models.dart';
import 'chat_screen.dart';

class ArchiveDetailScreen extends StatefulWidget {
  final ApiClient api;
  final String token;
  final LovedOne lovedOne;

  const ArchiveDetailScreen({
    super.key,
    required this.api,
    required this.token,
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
      appBar: AppBar(title: Text('${_current.name} 的档案')),
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
          const ListTile(
            leading: Icon(Icons.verified_user_outlined),
            title: Text('知情同意记录'),
            subtitle: Text('即将支持'),
            enabled: false,
          ),
        ],
      ),
    );
  }

  Widget _mediaCard(MediaItem item) {
    final sizeLabel = item.sizeBytes == null
        ? ''
        : ' · ${(item.sizeBytes! / 1024).toStringAsFixed(1)}KB';
    return Tooltip(
      message: item.objectKey ?? item.mediaType,
      child: Container(
        width: 110,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade300),
          borderRadius: BorderRadius.circular(8),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
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
      ),
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
