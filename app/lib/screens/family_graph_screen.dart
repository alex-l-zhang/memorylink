import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models.dart';
import '../relation_options.dart';

/// 以自己为中心的家族关系图谱。
/// 默认只展示已确认的家人；可切换显示半透明的"待确认"节点。
class FamilyGraphScreen extends StatefulWidget {
  final ApiClient api;
  final String token;

  const FamilyGraphScreen({super.key, required this.api, required this.token});

  @override
  State<FamilyGraphScreen> createState() => _FamilyGraphScreenState();
}

class _FamilyGraphScreenState extends State<FamilyGraphScreen> {
  RelationGraph? _graph;
  bool _includePending = false;
  bool _includeExtended = false;
  bool _loading = true;
  bool _changed = false;
  String? _error;
  GraphNode? _hovered;

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
      final graph =
          await widget.api.relationGraph(
        widget.token,
        includePending: _includePending,
        includeExtended: _includeExtended,
      );
      if (!mounted) return;
      setState(() => _graph = graph);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '加载失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openNode(GraphNode node) async {
    if (node.isSelf) {
      _showSelfInfo(node);
      return;
    }
    if (node.extended) {
      final requested = await showModalBottomSheet<bool>(
        context: context,
        isScrollControlled: true,
        builder: (_) => _ExtendedNodeSheet(
          api: widget.api,
          token: widget.token,
          node: node,
        ),
      );
      if (requested == true) {
        _changed = true;
        await _reload();
      }
      return;
    }
    final changed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _NodeSheet(api: widget.api, token: widget.token, node: node),
    );
    if (changed == true) {
      _changed = true;
      await _reload();
    }
  }

  void _showSelfInfo(GraphNode node) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('${node.name}（我）'),
        content: Text([
          '出生年月：${_birthText(node)}',
          '籍贯：${node.birthPlace ?? '未填写'}',
        ].join('\n')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('知道了')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        Navigator.of(context).pop(_changed);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('家族关系图谱'),
          actions: [
            IconButton(
              tooltip: '刷新',
              icon: const Icon(Icons.refresh),
              onPressed: _loading ? null : _reload,
            ),
          ],
        ),
        body: Column(
          children: [
            SwitchListTile(
              value: _includePending,
              title: const Text('显示待确认节点'),
              subtitle: Text(_graph == null
                  ? '半透明节点表示关系尚未双方确认'
                  : '半透明节点：${_graph!.pendingCount} 个待确认'),
              onChanged: (value) {
                setState(() => _includePending = value);
                _reload();
              },
            ),
            SwitchListTile(
              value: _includeExtended,
              title: const Text('显示家人的家人'),
              subtitle: Text(_graph == null
                  ? '虚线节点：经由我已确认的家人关联到的人'
                  : '虚线节点：${_extendedCount()} 位家人的家人，可点开发起建立联系'),
              onChanged: (value) {
                setState(() => _includeExtended = value);
                _reload();
              },
            ),
            const Divider(height: 1),
            Expanded(child: _buildBody()),
          ],
        ),
      ),
    );
  }

  int _extendedCount() =>
      _graph?.nodes.where((node) => node.extended).length ?? 0;

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
    final graph = _graph;
    if (graph == null) return const SizedBox.shrink();
    return Column(
      children: [
        if (_hovered != null && !_hovered!.isSelf) _hoverCard(_hovered!),
        Expanded(
          child: LayoutBuilder(
            builder: (context, constraints) {
              final size = Size(constraints.maxWidth, math.max(constraints.maxHeight, 360));
              return SingleChildScrollView(
                child: SizedBox(
                  width: size.width,
                  height: math.max(size.height, 420),
                  child: _GraphCanvas(
                    graph: graph,
                    onHover: (node) => setState(() => _hovered = node),
                    onTap: _openNode,
                  ),
                ),
              );
            },
          ),
        ),
        if (graph.nodes.isEmpty)
          const Padding(
            padding: EdgeInsets.all(16),
            child: Text('还没有可展示的家人关系。可通过"联系家人"或邀请码建立联系。'),
          ),
      ],
    );
  }

  Widget _hoverCard(GraphNode node) {
    return Container(
      width: double.infinity,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Text(
        node.extended
            ? '${node.name} · ${_pathSummary(node)} · 你与 TA 还没有建立关系'
            : '${node.name} · ${_relationSummary(node)} · ${_birthText(node)} · 籍贯：${node.birthPlace ?? '未填写'}',
        style: const TextStyle(fontSize: 12),
      ),
    );
  }
}

class _GraphCanvas extends StatelessWidget {
  final RelationGraph graph;
  final ValueChanged<GraphNode> onHover;
  final ValueChanged<GraphNode> onTap;

  const _GraphCanvas({
    required this.graph,
    required this.onHover,
    required this.onTap,
  });

  static const double nodeWidth = 108;
  static const double nodeHeight = 60;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final height = constraints.maxHeight;
        final center = Offset(width / 2, height / 2);
        final radiusX = math.max(80.0, width / 2 - nodeWidth / 2 - 8);
        final radiusY = math.max(80.0, height / 2 - nodeHeight / 2 - 16);
        final nodes = graph.nodes.where((node) => !node.extended).toList();
        final extendedNodes = graph.nodes.where((node) => node.extended).toList();
        final positions = <GraphNode, Offset>{};
        final angleByUser = <int, double>{};
        for (var i = 0; i < nodes.length; i++) {
          final angle = -math.pi / 2 + (2 * math.pi * i / math.max(nodes.length, 1));
          angleByUser[nodes[i].userId] = angle;
          positions[nodes[i]] = Offset(
            center.dx + radiusX * 0.64 * math.cos(angle),
            center.dy + radiusY * 0.64 * math.sin(angle),
          );
        }
        // 二级节点（家人的家人）：围绕其"路径中间人"的角度排在外圈
        final byVia = <int?, List<GraphNode>>{};
        for (final node in extendedNodes) {
          byVia.putIfAbsent(node.viaUserId, () => []).add(node);
        }
        final extendedPositions = <GraphNode, Offset>{};
        byVia.forEach((viaId, group) {
          final base = angleByUser[viaId] ?? -math.pi / 2;
          final step = 0.34;
          for (var i = 0; i < group.length; i++) {
            final angle = base + (i - (group.length - 1) / 2) * step;
            extendedPositions[group[i]] = Offset(
              center.dx + radiusX * math.cos(angle),
              center.dy + radiusY * math.sin(angle),
            );
          }
        });
        final positionByUser = <int, Offset>{
          for (final entry in positions.entries) entry.key.userId: entry.value,
          graph.self.userId: center,
        };
        final edges = <_Edge>[
          for (final entry in positions.entries)
            _Edge(center, entry.value, entry.key.pending, false),
          for (final entry in extendedPositions.entries)
            _Edge(
              positionByUser[entry.key.viaUserId] ?? center,
              entry.value,
              false,
              true,
            ),
        ];
        return Stack(
          children: [
            Positioned.fill(
              child: CustomPaint(
                painter: _EdgePainter(
                  edges: edges,
                  activeColor: Theme.of(context).colorScheme.outlineVariant,
                  pendingColor: Theme.of(context).colorScheme.outline,
                ),
              ),
            ),
            ...positions.entries.map((entry) => Positioned(
                  left: entry.value.dx - nodeWidth / 2,
                  top: entry.value.dy - nodeHeight / 2,
                  width: nodeWidth,
                  height: nodeHeight,
                  child: _NodeCard(
                    node: entry.key,
                    onHover: onHover,
                    onTap: () => onTap(entry.key),
                  ),
                )),
            ...extendedPositions.entries.map((entry) => Positioned(
                  left: entry.value.dx - nodeWidth / 2,
                  top: entry.value.dy - nodeHeight / 2,
                  width: nodeWidth,
                  height: nodeHeight,
                  child: _NodeCard(
                    node: entry.key,
                    onHover: onHover,
                    onTap: () => onTap(entry.key),
                  ),
                )),
            Positioned(
              left: center.dx - nodeWidth / 2,
              top: center.dy - nodeHeight / 2,
              width: nodeWidth,
              height: nodeHeight,
              child: _NodeCard(node: graph.self, onHover: onHover, onTap: () => onTap(graph.self)),
            ),
          ],
        );
      },
    );
  }
}

class _Edge {
  final Offset from;
  final Offset to;
  final bool pending;
  final bool dashed;

  _Edge(this.from, this.to, this.pending, this.dashed);
}

class _NodeCard extends StatelessWidget {
  final GraphNode node;
  final ValueChanged<GraphNode> onHover;
  final VoidCallback onTap;

  const _NodeCard({required this.node, required this.onHover, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = node.isSelf
        ? scheme.primaryContainer
        : node.extended
            ? scheme.surfaceContainerLow
            : scheme.surfaceContainerHighest;
    final relation = node.isSelf
        ? '我'
        : node.extended
            ? _pathLabel(node)
            : (node.relationFromMe == null ? '待确认' : relationLabel(node.relationFromMe));
    final card = Container(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(10),
        border: node.extended
            ? null
            : Border.all(
                color: node.pending ? scheme.outline : scheme.primary,
                width: node.isSelf ? 2 : 1,
              ),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                node.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
              ),
              if (node.extended) ...[
                const SizedBox(width: 4),
                Icon(Icons.link, size: 11, color: scheme.outline),
              ],
            ],
          ),
          Text(
            node.pending && !node.isSelf ? '$relation（待确认）' : relation,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 11, color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
    return MouseRegion(
      onEnter: (_) => onHover(node),
      onHover: (_) => onHover(node),
      cursor: SystemMouseCursors.click,
      child: Tooltip(
        message: '${node.name}（$relation）',
        child: Opacity(
          opacity: node.pending || node.extended ? 0.6 : 1,
          child: GestureDetector(
            onTap: onTap,
            child: node.extended
                ? CustomPaint(
                    foregroundPainter: _DashedBorderPainter(
                      color: scheme.outline,
                      radius: 10,
                    ),
                    child: card,
                  )
                : card,
          ),
        ),
      ),
    );
  }
}

/// 虚线圆角边框：用于"家人的家人"二级节点。
class _DashedBorderPainter extends CustomPainter {
  final Color color;
  final double radius;

  _DashedBorderPainter({required this.color, required this.radius});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = 1.2
      ..style = PaintingStyle.stroke;
    final path = Path()
      ..addRRect(RRect.fromRectAndRadius(Offset.zero & size, Radius.circular(radius)));
    const dash = 5.0;
    const gap = 4.0;
    for (final metric in path.computeMetrics()) {
      var distance = 0.0;
      while (distance < metric.length) {
        final next = math.min(distance + dash, metric.length);
        canvas.drawPath(metric.extractPath(distance, next), paint);
        distance = next + gap;
      }
    }
  }

  @override
  bool shouldRepaint(_DashedBorderPainter oldDelegate) =>
      oldDelegate.color != color || oldDelegate.radius != radius;
}

class _EdgePainter extends CustomPainter {
  final List<_Edge> edges;
  final Color activeColor;
  final Color pendingColor;

  _EdgePainter({
    required this.edges,
    required this.activeColor,
    required this.pendingColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final edge in edges) {
      final paint = Paint()
        ..color = edge.pending || edge.dashed
            ? pendingColor.withValues(alpha: 0.5)
            : activeColor
        ..strokeWidth = edge.pending || edge.dashed ? 1 : 1.6;
      if (edge.dashed) {
        _drawDashedLine(canvas, edge.from, edge.to, paint);
      } else {
        canvas.drawLine(edge.from, edge.to, paint);
      }
    }
  }

  void _drawDashedLine(Canvas canvas, Offset from, Offset to, Paint paint) {
    const dash = 6.0;
    const gap = 5.0;
    final total = (to - from).distance;
    if (total == 0) return;
    final direction = (to - from) / total;
    var travelled = 0.0;
    while (travelled < total) {
      final end = math.min(travelled + dash, total);
      canvas.drawLine(from + direction * travelled, from + direction * end, paint);
      travelled = end + gap;
    }
  }

  @override
  bool shouldRepaint(_EdgePainter oldDelegate) => oldDelegate.edges != edges;
}

/// 节点详情与关系操作：确认（可改称谓）/ 解除关系。
class _ExtendedNodeSheet extends StatefulWidget {
  final ApiClient api;
  final String token;
  final GraphNode node;

  const _ExtendedNodeSheet({
    required this.api,
    required this.token,
    required this.node,
  });

  @override
  State<_ExtendedNodeSheet> createState() => _ExtendedNodeSheetState();
}

class _ExtendedNodeSheetState extends State<_ExtendedNodeSheet> {
  String _relation = 'FRIEND';
  bool _busy = false;
  late bool _sent = widget.node.requestPending;

  Future<void> _send() async {
    setState(() => _busy = true);
    try {
      await widget.api.sendConnections(
        widget.token,
        targetIds: [widget.node.userId],
        relation: _relation,
      );
      if (!mounted) return;
      setState(() => _sent = true);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('已向${widget.node.name}发起建立联系，等待对方同意')),
      );
    } on ApiException catch (e) {
      _snack(e.message);
    } catch (_) {
      _snack('发送失败，请稍后重试');
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
    final node = widget.node;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(node.name, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('关系路径：${_pathLabel(node)}'),
            const SizedBox(height: 4),
            Text(
              '这是你家人的家人，你与 TA 还没有建立关系。发起后需要对方同意，'
              '同意后双方档案中才能互相看到。',
              style: TextStyle(fontSize: 12, color: Theme.of(context).colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            if (_sent)
              Row(
                children: [
                  const Icon(Icons.hourglass_top, size: 18),
                  const SizedBox(width: 6),
                  Expanded(child: Text('已发起建立联系，等待${node.name}同意')),
                ],
              )
            else ...[
              DropdownButtonFormField<String>(
                initialValue: _relation,
                decoration: const InputDecoration(
                  labelText: '我是 TA 的：',
                  border: OutlineInputBorder(),
                  isDense: true,
                ),
                items: relationOptions
                    .map((o) => DropdownMenuItem(value: o.code, child: Text(o.label)))
                    .toList(),
                onChanged: (value) {
                  if (value != null) setState(() => _relation = value);
                },
              ),
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: _busy ? null : _send,
                icon: const Icon(Icons.person_add_alt),
                label: Text(_busy ? '发送中…' : '发起建立联系'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _NodeSheet extends StatefulWidget {
  final ApiClient api;
  final String token;
  final GraphNode node;

  const _NodeSheet({required this.api, required this.token, required this.node});

  @override
  State<_NodeSheet> createState() => _NodeSheetState();
}

class _NodeSheetState extends State<_NodeSheet> {
  late String _relation = widget.node.relationFromMe ?? 'FAMILY';
  bool _busy = false;

  Future<void> _confirm() async {
    final relationshipId = widget.node.relationshipId;
    if (relationshipId == null) return;
    setState(() => _busy = true);
    try {
      await widget.api.confirmRelationship(widget.token, relationshipId, _relation);
      if (mounted) Navigator.pop(context, true);
    } on ApiException catch (e) {
      _snack(e.message);
    } catch (_) {
      _snack('操作失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _remove() async {
    final relationshipId = widget.node.relationshipId;
    if (relationshipId == null) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('解除关系'),
        content: Text('解除后你与「${widget.node.name}」在双方的档案中都不再显示，'
            '对方之后可以再次申请确认。'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('解除')),
        ],
      ),
    );
    if (ok != true) return;
    setState(() => _busy = true);
    try {
      await widget.api.removeRelationship(widget.token, relationshipId);
      if (mounted) Navigator.pop(context, true);
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
    final node = widget.node;
    final statusText = switch (node.myStatus) {
      'ACTIVE' => node.otherStatus == 'ACTIVE' ? '关系已双方确认' : '已确认，对方暂未确认',
      'PENDING' => '待我确认',
      'REJECTED' => '我已拒绝，当前不显示',
      _ => node.myStatus,
    };
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(node.name, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('对方是我的：${node.relationFromMe == null ? '待确认' : relationLabel(node.relationFromMe)}'),
            Text('我是对方的：${node.relationFromOther == null ? '待对方确认' : relationLabel(node.relationFromOther)}'),
            Text('出生年月：${_birthText(node)}'),
            Text('籍贯：${node.birthPlace ?? '未填写'}'),
            Text('性别：${_genderText(node.gender)}'),
            Text('状态：$statusText'),
            const SizedBox(height: 16),
            if (node.myStatus != 'ACTIVE' && node.relationshipId != null) ...[
              DropdownButtonFormField<String>(
                initialValue: _relation,
                decoration: const InputDecoration(
                  labelText: '对方是我的：',
                  border: OutlineInputBorder(),
                  isDense: true,
                ),
                items: relationOptions
                    .map((o) => DropdownMenuItem(value: o.code, child: Text(o.label)))
                    .toList(),
                onChanged: (value) {
                  if (value != null) setState(() => _relation = value);
                },
              ),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: _busy ? null : _confirm,
                child: const Text('确认关系'),
              ),
            ],
            if (node.myStatus == 'ACTIVE' && node.relationshipId != null)
              OutlinedButton.icon(
                onPressed: _busy ? null : _remove,
                icon: const Icon(Icons.link_off),
                label: const Text('解除关系'),
              ),
          ],
        ),
      ),
    );
  }
}

String _relationSummary(GraphNode node) {
  final fromMe = node.relationFromMe == null ? '待确认' : '对方是我的${relationLabel(node.relationFromMe)}';
  final fromOther =
      node.relationFromOther == null ? '我是对方的：待确认' : '我是对方的${relationLabel(node.relationFromOther)}';
  return '$fromMe / $fromOther';
}

/// 二级节点的关系路径，例如"张耘嫣的母亲"。
String _pathLabel(GraphNode node) {
  if (node.viaUserName == null) {
    return '家人的家人';
  }
  if (node.relationFromVia == null) {
    return '${node.viaUserName}的家人';
  }
  return '${node.viaUserName}的${relationLabel(node.relationFromVia)}';
}

String _pathSummary(GraphNode node) => _pathLabel(node);

String _birthText(GraphNode node) {
  if (node.birthYear == null) return '未填写';
  if (node.birthMonth == null) return '${node.birthYear} 年';
  return '${node.birthYear} 年 ${node.birthMonth} 月';
}

String _genderText(String? gender) {
  return switch (gender) {
    'MALE' => '男',
    'FEMALE' => '女',
    'OTHER' => '其他',
    _ => '未填写',
  };
}
