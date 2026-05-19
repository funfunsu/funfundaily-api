UPDATE universal_record
SET content = '[{"id":null,"itemTitle":"积分管理","itemType":"path","uri":"/subPackages/points/pages/points-product-manage","status":"active"},{"id":null,"itemTitle":"积分兑换","itemType":"path","uri":"/subPackages/points/pages/points-exchange","status":"active"},{"id":null,"itemTitle":"汉字书写","itemType":"path","uri":"/subPackages/study-tools/pages/writing/stroke-order","status":"active"},{"id":null,"itemTitle":"理财计划","itemType":"path","uri":"/subPackages/financial-plan/pages/list/index","status":"active"}, {"id": null, "uri": "/subPackages/study-tools/pages/ledger/index", "status": "active", "itemType": "path", "itemTitle": "家庭账本"}]'
WHERE scene = 'system'
  AND scene_var = 'discovery'
  AND business_key = 'default';