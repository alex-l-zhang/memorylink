import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models.dart';
import '../relation_options.dart';

/// 消息中心：家族关系确认等站内消息，带未读角标与批量确认。
class MessageCenterScreen extends StatefulWidget {
  final ApiClient api;
  final String token;

  const MessageCenterScreen({super.key, required this.api, required this.token});

  @override
  State<MessageCenterScreen> createState() => _MessageCenterScreenState();
}

class _MessageCenterScreenState extends State<MessageCenterScreen> {
  List<NotificationItem> _items = [];
  final Map<int, String> _selection = {};
  bool _loading = true;
  bool _busy = false;
  String? _error;
  bool _changed = false;

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
      final list = await widget.api.messages(widget.token);
      if (!mounted) return;
      setState(() {
        _items = list.items;
        for (final item in list.items) {
          _selection.putIfAbsent(
            item.id,
            () => item.suggestedRelation ?? 'FAMILY',
          );
        }
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '加载失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _confirm(NotificationItem item) async {
    final relation = _selection[item.id] ?? 'FAMILY';
    await _run(() => widget.api.confirmMessage(widget.token, item.id, relation), '已确认关系');
  }

  Future<void> _reject(NotificationItem item) async {
    await _run(() => widget.api.rejectMessage(widget.token, item.id), '已拒绝，关系暂不显示');
  }

  Future<void> _confirmAll() async {
    final pending = _items.where((e) => e.type == 'RELATION_CONFIRM' && e.pending).length;
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('全部确认为家人'),
        content: Text('将按系统建议确认 $pending 条关系，未给出建议的记为"家人"。\n'
            '确认后如称谓不准确，可在关系图谱里修改。'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('确认')),
        ],
      ),
    );
    if (ok != true) return;
    await _run(() async {
      final count = await widget.api.confirmAllMessages(widget.token);
      if (mounted && count > 0) _snack('已确认 $count 条关系');
    }, null);
  }

  Future<void> _run(Future<void> Function() action, String? successMessage) async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      await action();
      _changed = true;
      if (successMessage != null) _snack(successMessage);
      await _reload();
    } on ApiException catch (e) {
      _snack(e.message);
    } catch (_) {
      _snack('操作失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _snack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final pendingCount =
        _items.where((e) => e.type == 'RELATION_CONFIRM' && e.pending).length;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        Navigator.of(context).pop(_changed);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('消息中心'),
          actions: [
            if (pendingCount > 0)
              TextButton(
                onPressed: _busy ? null : _confirmAll,
                child: const Text('全部确认为家人'),
              ),
          ],
        ),
        body: _buildBody(),
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
    if (_items.isEmpty) {
      return const Center(child: Text('暂无消息'));
    }
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: _items.length,
        itemBuilder: (context, index) => _buildItem(_items[index]),
      ),
    );
  }

  Widget _buildItem(NotificationItem item) {
    final pending = item.pending;
    final statusText = switch (item.status) {
      'UNREAD' || 'READ' => '待处理',
      'ACTIONED' => '已确认',
      'REJECTED' => '已拒绝',
      _ => item.status,
    };
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(item.title,
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
                Chip(
                  label: Text(statusText),
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
            if (item.body != null) ...[
              const SizedBox(height: 4),
              Text(item.body!),
            ],
            if (item.otherUserId != null) ...[
              const SizedBox(height: 4),
              Text('来自：${item.otherName ?? '家族成员'}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey)),
            ],
            if (pending) ...[
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _selection[item.id] ?? 'FAMILY',
                decoration: const InputDecoration(
                  labelText: '对方是我的：',
                  border: OutlineInputBorder(),
                  isDense: true,
                ),
                items: relationOptions
                    .map((o) => DropdownMenuItem(value: o.code, child: Text(o.label)))
                    .toList(),
                onChanged: (value) {
                  if (value != null) setState(() => _selection[item.id] = value);
                },
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  FilledButton(
                    onPressed: _busy ? null : () => _confirm(item),
                    child: const Text('确认关系'),
                  ),
                  const SizedBox(width: 8),
                  TextButton(
                    onPressed: _busy ? null : () => _reject(item),
                    child: const Text('不确认（保持隐藏）'),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
