(ns top.kzre.krro.canvas.core.layer.path
  "图层路径：图层或图层组在图层树中的位置标识。
   薄包装层，委托 top.kzre.krro.core.util.tree-path 完成通用路径操作。

   路径是索引向量，如 [2 0] 表示根列表第 2 个图层的第 0 个子图层。
   空向量 [] 表示根容器本身。

   ═══════════════════════════════════════════════
   设计原则
   ═══════════════════════════════════════════════

   本命名空间把\"图层操作\"分解为两段：

     第一段：id ↔ path
       用户持有 layer-id——通过 get-path 得到路径
       或反向：路径 → 图层 → 取 id

     第二段：path → path（导航）+ path → layer（访问）
       路径之间可以导航（上/下兄弟、父子、遍历）
       路径可以访问图层、兄弟列表、子路径

   外部操作只针对路径：
     - 不直接操作图层树
     - 不关心嵌套结构
     - 通过 path 完成所有定位

   这样图层操作的复杂度从\"树操作\"降为\"向量操作 + 树查询\"——
   路径是稳定的中间抽象。

   ═══════════════════════════════════════════════
   使用模式
   ═══════════════════════════════════════════════

     ;; id → path
     (def p (get-path layers :layer-42))

     ;; path → layer
     (get-layer layers p)

     ;; path → path（导航）
     (above-sibling-path layers p)
     (parent p)

     ;; path → 其他图层
     (get-layer layers (above-sibling-path layers p))

   参数顺序遵循 Clojure 惯例：layers 优先。
   检索函数统一使用 get- 前缀。

   ═══════════════════════════════════════════════
   职责边界
   ═══════════════════════════════════════════════

   本命名空间：
     ✓ 路径构造、变换、比较（委托 tree-path）
     ✓ 按 id 查路径、按路径查图层（包装 :layers children-fn）
     ✓ 导航（父、子、兄弟、遍历）（委托 + 包装）
     ✓ 图层树修改（包装 tree-path 的 insert/remove/move/update-node）

   不属于本命名空间：
     ✗ 通用树路径逻辑——委托 top.kzre.krro.core.util.tree-path
     ✗ 图层属性的读取/修改——属于图层操作命名空间
     ✗ 图层的语义判断（可见性、类型）——属于图层定义"

  (:require [top.kzre.krro.core.util.tree-path :as tp]))

;; ═══════════════════════════════════════════════
;; children-fn / assoc-children-fn
;; ═══════════════════════════════════════════════

(def ^:private layers-children
  "图层树的 children-fn，从节点取 :layers。"
  :layers)

(def ^:private assoc-layers
  "将子图层列表写回节点。"
  (fn [node children] (assoc node :layers children)))

;; ═══════════════════════════════════════════════
;; 路径构造（委托 tree-path）
;; ═══════════════════════════════════════════════

(def ^:deprecated root tp/root)
(def ^:deprecated child tp/child)
(def ^:deprecated append tp/append)
(def ^:deprecated parent tp/parent)
(def ^:deprecated with-index tp/with-index)
(def ^:deprecated truncate tp/truncate)
(def ^:deprecated path? tp/path?)
(def ^:deprecated root? tp/root?)
(def ^:deprecated depth tp/depth)
(def ^:deprecated index-in-parent tp/index-in-parent)

;; ═══════════════════════════════════════════════
;; 层级关系（委托 tree-path）
;; ═══════════════════════════════════════════════

(def ^:deprecated ancestor? tp/ancestor?)
(def ^:deprecated descendant? tp/descendant?)
(def ^:deprecated sibling? tp/sibling?)
(def ^:deprecated within? tp/within?)
(def ^:deprecated common-ancestor tp/common-ancestor)
(def ^:deprecated path< tp/path<)

;; ═══════════════════════════════════════════════
;; 第一段：id ↔ path
;; ═══════════════════════════════════════════════

(defn get-path
  "查找指定 layer-id 的索引路径。未找到返回 nil。
   返回向量（不是 seq）。"
  [layers layer-id]
  (tp/get-path-by-id layers layer-id layers-children))

;; ═══════════════════════════════════════════════
;; 第二段：path → layer
;; ═══════════════════════════════════════════════

(defn get-layer
  "按索引路径查找图层。路径无效返回 nil。"
  [layers path]
  (tp/get-node layers path layers-children))

(defn get-siblings
  "路径所指向的兄弟图层，包括自己。
   空路径返回 layers 本身。"
  [layers path]
  (tp/children layers path layers-children))

(defn child-paths
  "父路径的所有子路径——升序。"
  [layers parent-path]
  (tp/child-paths layers parent-path layers-children))

(defn first-child-path
  "父路径的第一个子路径。无子返回 nil。"
  [layers parent-path]
  (tp/first-child-path layers parent-path layers-children))

(defn last-child-path
  "父路径的最后一个子路径。无子返回 nil。"
  [layers parent-path]
  (tp/last-child-path layers parent-path layers-children))

(defn above-last-child-path
  "父路径的最后一个子路径之后的虚拟路径。"
  [layers parent-path]
  (tp/above-last-child-path layers parent-path layers-children))

;; ═══════════════════════════════════════════════
;; 第二段：path → path（导航）
;; ═══════════════════════════════════════════════

(defn ^:deprecated top-path
  "顶层（末尾）路径。列表为空返回 nil。"
  [layers]
  (tp/top-path layers))

(defn ^:deprecated above-top-path
  "最顶层图层之上的虚拟路径。"
  [layers]
  (tp/above-top-path layers))

(defn ^:deprecated bottom-path
  "底层（索引 0）路径。列表为空返回 nil。"
  [layers]
  (tp/bottom-path layers))

(defn above-sibling-path
  "同一父容器中当前路径的下一个兄弟路径。已是最后一个返回 nil。"
  [layers path]
  (tp/above-sibling-path layers path layers-children))

(defn ^:deprecated unchecked-above-sibling-path
  "返回在 layer-id 上方插入新图层时应使用的路径。"
  [path]
  (tp/unchecked-above-sibling-path path))

(defn above-sibling-paths
  "同一父容器中，当前路径上方（索引更大）的所有兄弟路径。索引升序。"
  [layers path]
  (tp/above-sibling-paths layers path layers-children))

(defn below-sibling-path
  "同一父容器中当前路径的前一个兄弟路径。已是第一个返回 nil。"
  [layers path]
  (tp/below-sibling-path layers path layers-children))

(defn below-sibling-paths
  "同一父容器中，当前路径下方（索引更小）的所有兄弟路径。索引升序。"
  [layers path]
  (tp/below-sibling-paths layers path layers-children))

(defn ^:deprecated sibling-range-paths
  "同一父下，从 from-idx 到 to-idx（不含）的所有路径。"
  [parent-path from-idx to-idx]
  (tp/sibling-range-paths parent-path from-idx to-idx))

(defn ^:deprecated paths-between
  "同一父下，从 a 到 b（不含 b）的所有路径。a、b 必须同父。"
  [a b]
  (tp/paths-between a b))

(defn ^:deprecated next-path
  "按 DFS 前序遍历，下一个路径。"
  [layers path]
  (tp/next-path layers path layers-children))

(defn prev-path
  "按 DFS 前序遍历，前一个路径。"
  [layers path]
  (tp/prev-path layers path layers-children))

;; ═══════════════════════════════════════════════
;; 图层树修改（包装 tree-path 通用函数）
;; ═══════════════════════════════════════════════

(defn insert-layer
  "在图层列表 layers 的指定路径处插入 layer。返回新的图层向量。
   path 为索引向量，如 [2] 表示根索引 2，[2 0] 表示组内索引。
   空路径表示插入到末尾。"
  [layers path layer]
  (tp/insert-node layers path layer layers-children assoc-layers))

(defn remove-layer
  "从图层列表 layers 的指定路径移除图层。返回 [new-layers removed-layer]。
   path 为索引向量。"
  [layers path]
  (tp/remove-node layers path layers-children assoc-layers))

(defn move-layer
  "将图层从 old-path 移动到 new-path，返回新的图层列表。
   old-path 和 new-path 均为索引向量。"
  [layers old-path new-path]
  (tp/move-node layers old-path new-path layers-children assoc-layers))

(defn update-layer
  "对指定路径处的图层应用更新函数 f，返回新的图层列表。
   f 接收当前图层，返回更新后的图层。
   路径无效时返回原 layers。"
  [layers path f]
  (tp/update-node layers path f layers-children assoc-layers))

;; ═══════════════════════════════════════════════
;; 排序（委托 tree-path）
;; ═══════════════════════════════════════════════

(def ^:deprecated sort-paths tp/sort-paths)

(defn ^:deprecated sort-by-path
  "paths 和 attachments 是一一对应的扁平序列。
   按 paths 的 DFS 前序排序，attachments 跟随移动。
   返回 [sorted-paths sorted-attachments]。"
  [paths attachments]
  (tp/sort-by-path paths attachments))