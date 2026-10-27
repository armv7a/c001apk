# Digital categories and brands

Verified against the category API and a Reqable HAR captured on 2026-10-27.
Fixtures under `app/src/test/resources/product-catalog` contain only public
category, brand and sorting fields. Request headers and account data are excluded.

- `/v6/product/categoryList` returns top-level categories with optional
  `secondCategoryRows`. Both levels provide their own `/page?url=...` destination.
- A subcategory uses `isSecondCategory=1`. An "Other" child can share its parent's
  ID and instead use `filterOther=true`; use the complete URL as the selection key.
- `/v6/page/dataList` accepts the supplied destination as its `url`, plus the
  existing `title`, `page` and `lastItem` parameters.
- Category results include `iconGridHorizonCard` with `entityId=category-detail`.
  Its entities are brands. For example, the Xiaomi link from Power Banks retains
  `category_id=1021`, `isSecondCategory=1`, `type=brand` and `showTab=1`.
- Product sorting uses `sortSelectCard` with `entityId=productCategorySortSelect`
  or `productSeriesSortSelect`. The links contain `hot_num`, `star_average_score`
  or `release_time`; score links also include `limitField=star_total_count-80`.
  Do not replace these with feed-sorting parameters.
- Brand listings can declare their initial selection through
  `extraDataArr.selectedTab`. An explicit `sortField` takes precedence.
- Sorted responses may contain only products, even on page 1. Preserve the
  original brand/sort controls across sorting, pagination, empty results and
  failed requests. Header cards are separated from product rows for rendering.

The digital sidebar expands only the active parent's children. Clicking the
parent returns to its full category; switching categories collapses the previous
children. Each destination keeps its own list state. Brands and sorting scroll
with the content. Brand links use the existing app navigation, while sorting
replaces the current list and resets pagination.
