import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
