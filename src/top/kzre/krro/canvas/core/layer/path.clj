(ns top.kzre.krro.canvas.core.layer.path
  (:require [top.kzre.krro.core.util.tree-path :as tp]))

;; ═══════════════════════════════════════════════
;; 图层树的节点访问约定
;; ═══════════════════════════════════════════════

(def ^:private children-fn :layers)

(def ^:private assoc-children-fn
  (fn [node new-children]
    (assoc node :layers new-children)))

(def ^:private id-fn :id)

;; ═══════════════════════════════════════════════
;; 路径构造 / 层级关系 / 排序 —— 直接委托
;; ═══════════════════════════════════════════════

(def ^:deprecated root       tp/root)
(def ^:deprecated child      tp/child)
(def ^:deprecated append     tp/append)
(def ^:deprecated parent     tp/parent)
(def ^:deprecated with-index tp/with-index)
(def ^:deprecated truncate   tp/truncate)
(def ^:deprecated path?      tp/path?)
(def ^:deprecated ^:deprecated root?      tp/root?)

(def ^:deprecated ancestor?       tp/ancestor?)
(def ^:deprecated descendant?     tp/descendant?)
(def ^:deprecated sibling?        tp/sibling?)
(def ^:deprecated ^:deprecated within?         tp/within?)
(def ^:deprecated common-ancestor tp/common-ancestor)
(def ^:deprecated depth           tp/depth)
(def ^:deprecated index-in-parent tp/index-in-parent)
(def ^:deprecated path<           tp/path<)

(def ^:deprecated top-path                     tp/top-path)
(def ^:deprecated above-top-path               tp/above-top-path)
(def ^:deprecated bottom-path                  tp/bottom-path)
(def ^:deprecated unchecked-above-sibling-path tp/unchecked-above-sibling-path)
(def ^:deprecated below-sibling-path           tp/below-sibling-path)
(def ^:deprecated sibling-range-paths          tp/sibling-range-paths)
(def ^:deprecated paths-between                tp/paths-between)

(def ^:deprecated sort-paths   tp/sort-paths)
(def ^:deprecated sort-by-path tp/sort-by-path)

;; ═══════════════════════════════════════════════
;; 第一段：id ↔ path
;; ═══════════════════════════════════════════════

(defn get-path
  "查找指定 layer-id 的索引路径。未找到返回 nil。"
  [layers layer-id]
  (tp/get-path-by-id layers layer-id id-fn children-fn))

;; ═══════════════════════════════════════════════
;; 第二段：path → layer
;; ═══════════════════════════════════════════════

(defn get-layer
  "按索引路径查找图层。路径无效返回 nil。空路径返回 layers 本身。"
  [layers path]
  (tp/get-node layers path children-fn))

(defn get-siblings
  "路径所指向的兄弟图层，包括自己。空路径返回 layers 本身。"
  [layers path]
  (tp/get-siblings layers path children-fn))

(defn child-paths
  "父路径的所有子路径——升序。空路径返回根级子路径。"
  [layers parent-path]
  (tp/child-paths layers parent-path children-fn))

(defn first-child-path
  "父路径的第一个子路径。无子返回 nil。"
  [layers parent-path]
  (tp/first-child-path layers parent-path children-fn))

(defn last-child-path
  "父路径的最后一个子路径。无子返回 nil。"
  [layers parent-path]
  (tp/last-child-path layers parent-path children-fn))

(defn above-last-child-path
  "父路径的最后一个子路径之后的虚拟路径。"
  [layers parent-path]
  (tp/above-last-child-path layers parent-path children-fn))

;; ═══════════════════════════════════════════════
;; 第二段：path → path（导航）
;; ═══════════════════════════════════════════════

(defn above-sibling-path
  "同一父容器中当前路径的下一个兄弟路径。已是最后一个返回 nil。"
  [layers path]
  (tp/above-sibling-path layers path children-fn))

(defn above-sibling-paths
  "同一父容器中，当前路径上方（索引更大）的所有兄弟路径。
   按从下到上的顺序返回（索引升序）。"
  [layers path]
  (tp/above-sibling-paths layers path children-fn))

(defn below-sibling-paths
  "同一父容器中，当前路径下方（索引更小）的所有兄弟路径。
   按从底到上的顺序返回（索引升序）。"
  [layers path]
  (tp/below-sibling-paths layers path children-fn))

(defn next-path
  "按 DFS 前序遍历，下一个路径。遍历结束返回 nil。"
  [layers path]
  (tp/next-path layers path children-fn))

(defn prev-path
  "按 DFS 前序遍历，前一个路径。"
  [layers path]
  (tp/prev-path layers path children-fn))

;; ═══════════════════════════════════════════════
;; 数据更新
;; ═══════════════════════════════════════════════

(defn insert-layer
  "在图层列表 layers 的指定路径处插入 layer。返回新的图层向量。
   path 为空表示插入到末尾。"
  [layers path layer]
  (tp/insert-node layers path layer children-fn assoc-children-fn))

(defn remove-layer
  "从图层列表 layers 的指定路径移除图层。返回 [new-layers removed-layer]。"
  [layers path]
  (tp/remove-node layers path children-fn assoc-children-fn))

(defn move-layer
  "将图层从 old-path 移动到 new-path，返回新的图层列表。"
  [layers old-path new-path]
  (tp/move-node layers old-path new-path children-fn assoc-children-fn))

(defn update-layer
  "对指定路径处的图层应用更新函数 f，返回新的图层列表。"
  [layers path f]
  (tp/update-node layers path f children-fn assoc-children-fn))