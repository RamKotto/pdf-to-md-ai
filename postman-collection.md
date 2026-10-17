# Postman Collections

## Коллекция для `pdf-to-md` (`pdf-to-md.postman_collection.json`)
```json
{
	"info": {
		"name": "pdf-to-md",
		"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
	},
	"item": [
		{
			"name": "Конвертировать PDF файл",
			"request": {
				"method": "POST",
				"header": [],
				"body": {
					"mode": "formdata",
					"formdata": [
						{
							"key": "file",
							"type": "file",
							"src": "/path/to/your/document.pdf"
						}
					]
				},
				"url": {
					"raw": "http://localhost:8080/api/convert",
					"protocol": "http",
					"host": ["localhost"],
					"port": "8080",
					"path": ["api", "convert"]
				}
			}
		},
		{
			"name": "Конвертировать по ссылке",
			"request": {
				"method": "POST",
				"header": [
					{
						"key": "Content-Type",
						"value": "application/json"
					}
				],
				"body": {
					"mode": "raw",
					"raw": "{\n  \"url\": \"https://raw.githubusercontent.com/example/api/main/openapi.yaml\",\n  \"filename\": \"swagger.yaml\"\n}"
				},
				"url": {
					"raw": "http://localhost:8080/api/convert",
					"protocol": "http",
					"host": ["localhost"],
					"port": "8080",
					"path": ["api", "convert"]
				}
			}
		}
	]
}
