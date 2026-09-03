import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../models.dart';

class MyOralScreen extends StatefulWidget {
  final ApiClient api;
  final String token;

  const MyOralScreen({super.key, required this.api, required this.token});

  @override
  State<MyOralScreen> createState() => _MyOralScreenState();
}

class _MyOralScreenState extends State<MyOralScreen> {
  List<OralHistoryItem> _orals = [];
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
      final orals = await widget.api.listMyOralHistories(widget.token);
      if (!mounted) return;
      setState(() => _orals = orals);
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

  Future<String?> _askTitle() async {
    final controller = TextEditingController();
    final title = await showDialog<String>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('口述标题（可留空）'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(hintText: '例如：小时候的夏天'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('下一步'),
          ),
        ],
      ),
    );
    controller.dispose();
    return title;
  }

  Future<void> _upload(String mediaType) async {
    final title = await _askTitle();
    if (!mounted) return;
    try {
      final file = await FilePicker.pickFile(
        type: mediaType == 'AUDIO' ? FileType.audio : FileType.video,
      );
      if (file == null) return;
      final bytes = await file.readAsBytes();
      setState(() => _uploading = true);
      await widget.api.uploadMyOralHistory(
        widget.token,
        mediaType: mediaType,
        title: title,
        filename: file.name.isEmpty ? 'oral' : file.name,
        bytes: bytes,
      );
      if (!mounted) return;
      await _reload();
      _showSnack('口述已上传（默认仅自己可见）');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('上传失败，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  Future<void> _toggleVisibility(OralHistoryItem item) async {
    final next = item.visibility == 'SELF_ONLY' ? 'FAMILY' : 'SELF_ONLY';
    try {
      await widget.api.updateOralVisibility(widget.token, item.lovedOneId, item.id, next);
      await _reload();
      _showSnack('可见性已更新');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('更新失败，请重试');
    }
  }

  Future<void> _delete(OralHistoryItem item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('删除这段口述？'),
        content: const Text('删除后不可恢复，AI 引用会同步清除。'),
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
      await widget.api.deleteOralHistory(widget.token, item.lovedOneId, item.id);
      await _reload();
      _showSnack('已删除');
    } on ApiException catch (e) {
      _showSnack(e.message);
    } catch (_) {
      _showSnack('删除失败，请重试');
    }
  }

  Future<void> _play(OralHistoryItem item) async {
    final url = item.url;
    if (url == null || url.isEmpty) {
      _showSnack('暂无可播放地址');
      return;
    }
    try {
      await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
    } catch (_) {
      _showSnack('无法打开播放，请复制链接后在浏览器打开');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的讲述')),
      floatingActionButton: _uploading
          ? null
          : FloatingActionButton.extended(
              onPressed: () => _upload('AUDIO'),
              icon: const Icon(Icons.mic_none),
              label: const Text('添加录音'),
            ),
      body: RefreshIndicator(
        onRefresh: _reload,
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
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
    if (_uploading) return const Center(child: CircularProgressIndicator());
    if (_orals.isEmpty) {
      return ListView(
        children: const [
          Padding(
            padding: EdgeInsets.all(32),
            child: Center(child: Text('还没有你的讲述。点击右下角"添加录音"录下第一段故事（默认仅自己可见）。')),
          ),
        ],
      );
    }
    return ListView.builder(
      itemCount: _orals.length,
      itemBuilder: (context, index) {
        final item = _orals[index];
        final visLabel = item.visibility == 'SELF_ONLY' ? '仅自己可见' : '家族可见';
        return ListTile(
          leading: CircleAvatar(
            child: Icon(item.mediaType == 'VIDEO' ? Icons.videocam : Icons.mic),
          ),
          title: Text(item.title == null || item.title!.isEmpty ? '未命名口述' : item.title!),
          subtitle: Text('${item.mediaType == 'VIDEO' ? '视频' : '录音'} · $visLabel'),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              IconButton(
                tooltip: '切换可见性',
                icon: Icon(
                  item.visibility == 'SELF_ONLY' ? Icons.lock_outline : Icons.group_outlined,
                ),
                onPressed: () => _toggleVisibility(item),
              ),
              IconButton(
                tooltip: '删除',
                icon: const Icon(Icons.close),
                onPressed: () => _delete(item),
              ),
            ],
          ),
          onTap: () => _play(item),
        );
      },
    );
  }
}
