import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../models.dart';

class MyAssetsScreen extends StatefulWidget {
  final ApiClient api;
  final String token;

  const MyAssetsScreen({super.key, required this.api, required this.token});

  @override
  State<MyAssetsScreen> createState() => _MyAssetsScreenState();
}

class _MyAssetsScreenState extends State<MyAssetsScreen> {
  MySelfPerson? _self;
  List<MediaItem> _media = [];
  List<ConsentRecord> _consents = [];
  bool _loading = true;
  bool _uploading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final selfFuture = widget.api.getMySelfPerson(widget.token);
      final mediaFuture = widget.api.listMyMedia(widget.token);
      final consentsFuture = widget.api.listMyConsents(widget.token);
      final results = await Future.wait([selfFuture, mediaFuture, consentsFuture]);
      if (!mounted) return;
      setState(() {
        _self = results[0] as MySelfPerson?;
        _media = results[1] as List<MediaItem>;
        _consents = results[2] as List<ConsentRecord>;
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '加载失败，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _upload(String mediaType) async {
    try {
      final file = await FilePicker.pickFile(
        type: mediaType == 'PHOTO'
            ? FileType.image
            : mediaType == 'AUDIO'
                ? FileType.audio
                : FileType.video,
      );
      if (file == null) return;
      final bytes = await file.readAsBytes();
      setState(() => _uploading = true);
      await widget.api.uploadMyMedia(
        widget.token,
        mediaType: mediaType,
        filename: file.name.isEmpty ? 'upload' : file.name,
        bytes: bytes,
      );
      if (!mounted) return;
      await _reload();
      _showSnack('素材已上传到我的档案');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('上传失败，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  Future<void> _toggleAi(bool enable) async {
    final self = _self;
    if (self == null) return;
    try {
      final enabled = enable
          ? await widget.api.enableAi(widget.token, self.id)
          : !await widget.api.disableAi(widget.token, self.id);
      if (!mounted) return;
      setState(() => _self = MySelfPerson(
            id: self.id,
            name: self.name,
            isDeceased: self.isDeceased,
            aiPersonaEnabled: enabled,
          ));
      _showSnack(enable ? 'AI 讲述已开启' : 'AI 讲述已关闭');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('操作失败，请稍后重试');
    }
  }

  Future<void> _deleteMedia(MediaItem item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('删除这条素材？'),
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
      await widget.api.deleteMyMedia(widget.token, item.id);
      await _reload();
      _showSnack('已删除');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('删除失败，请重试');
    }
  }

  void _openMedia(MediaItem item) async {
    final url = item.url;
    if (url == null || url.isEmpty) {
      _showSnack('暂无可访问地址');
      return;
    }
    if (item.mediaType == 'PHOTO') {
      showDialog<void>(
        context: context,
        builder: (dialogContext) => Dialog(
          backgroundColor: Colors.black,
          insetPadding: const EdgeInsets.all(12),
          child: GestureDetector(
            onTap: () => Navigator.of(dialogContext).pop(),
            child: InteractiveViewer(
              minScale: 0.8,
              maxScale: 5,
              child: SizedBox(
                width: MediaQuery.of(dialogContext).size.width - 24,
                height: MediaQuery.of(dialogContext).size.height * 0.8,
                child: Image.network(
                  url,
                  fit: BoxFit.contain,
                  errorBuilder: (_, _, _) => const Center(
                    child: Text('图片加载失败', style: TextStyle(color: Colors.white70)),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
    } else {
      try {
        await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
      } catch (_) {
        _showSnack('无法打开，请在浏览器中访问');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的素材与记录')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(_error!),
                      const SizedBox(height: 12),
                      FilledButton(onPressed: _reload, child: const Text('重试')),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _reload,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Text('AI 讲述', style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 8),
                      Card(
                        child: ListTile(
                          leading: Icon(
                            (_self?.aiPersonaEnabled ?? false)
                                ? Icons.record_voice_over
                                : Icons.voice_over_off,
                            color: (_self?.aiPersonaEnabled ?? false) ? Colors.green : Colors.grey,
                          ),
                          title: Text((_self?.aiPersonaEnabled ?? false)
                              ? 'AI 讲述已开启'
                              : 'AI 讲述未开启'),
                          subtitle: Text(_self == null
                              ? '上传素材或口述后会自动创建你的个人档案'
                              : '开启后，授权家人可基于你的讲述提问'),
                          trailing: _self == null
                              ? null
                              : (_self!.aiPersonaEnabled
                                  ? OutlinedButton(
                                      onPressed: () => _toggleAi(false),
                                      child: const Text('关闭'),
                                    )
                                  : FilledButton(
                                      onPressed: () => _toggleAi(true),
                                      child: const Text('开启'),
                                    )),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Row(
                        children: [
                          Text('我的素材', style: Theme.of(context).textTheme.titleMedium),
                          const Spacer(),
                          if (_uploading) const SizedBox(width: 100, child: LinearProgressIndicator()),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.tonalIcon(
                              onPressed: _uploading ? null : () => _upload('PHOTO'),
                              icon: const Icon(Icons.add_photo_alternate_outlined),
                              label: const Text('照片'),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: FilledButton.tonalIcon(
                              onPressed: _uploading ? null : () => _upload('AUDIO'),
                              icon: const Icon(Icons.mic_none),
                              label: const Text('录音'),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: FilledButton.tonalIcon(
                              onPressed: _uploading ? null : () => _upload('VIDEO'),
                              icon: const Icon(Icons.videocam_outlined),
                              label: const Text('视频'),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      if (_media.isEmpty)
                        const Text('还没有素材。')
                      else
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: _media.map((item) => _mediaCard(item)).toList(),
                        ),
                      const SizedBox(height: 20),
                      Text('我参与的授权记录', style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 8),
                      if (_consents.isEmpty)
                        const Text('暂无记录。')
                      else
                        ..._consents.map((c) => ListTile(
                              dense: true,
                              leading: const Icon(Icons.verified_user_outlined),
                              title: Text(c.consentType == 'PRE_AUTHORIZED' ? '故人生前预授权' : '两名近亲共同确认'),
                              subtitle: Text('记录 #${c.id} · 状态 ${c.status}'),
                            )),
                    ],
                  ),
                ),
    );
  }

  Widget _mediaCard(MediaItem item) {
    return GestureDetector(
      onTap: () => _openMedia(item),
      child: Stack(
        children: [
          Container(
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
                    item.mediaType == 'PHOTO'
                        ? '照片'
                        : item.mediaType == 'AUDIO'
                            ? '录音'
                            : '视频',
                    style: const TextStyle(fontSize: 11),
                  ),
                ),
              ],
            ),
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
    );
  }
}
