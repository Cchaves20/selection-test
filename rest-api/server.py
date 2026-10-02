"""API de dados simulados. Execute: python3 server.py."""
import json
import os
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit

PLATFORMS = 40
MODULES_PER_PLATFORM = 40
PHOTOS_PER_MODULE = 2000
MAX_LIMIT = 200


def platforms():
    return [{"id": i, "name": f"Plataforma {i:02d}"} for i in range(1, PLATFORMS + 1)]


def modules(platform_id):
    return [{"id": (platform_id - 1) * MODULES_PER_PLATFORM + i,
             "name": f"Módulo {i:02d}"} for i in range(1, MODULES_PER_PLATFORM + 1)]


def photos(module_id, offset, limit):
    end = min(offset + limit, PHOTOS_PER_MODULE)
    items = [{"id": (module_id - 1) * PHOTOS_PER_MODULE + i + 1,
              "moduleId": module_id, "name": f"Foto {i + 1:04d}",
              "url": f"https://picsum.photos/seed/module-{module_id}-photo-{i + 1}/800/600"}
             for i in range(offset, end)]
    return {"items": items, "offset": offset, "limit": limit,
            "total": PHOTOS_PER_MODULE, "hasMore": end < PHOTOS_PER_MODULE}


class Handler(BaseHTTPRequestHandler):
    def respond(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        request = urlsplit(self.path)
        if request.path == "/platforms":
            return self.respond(200, platforms())
        match = re.fullmatch(r"/platforms/([0-9]+)/modules", request.path)
        if match:
            platform_id = int(match[1])
            if not 1 <= platform_id <= PLATFORMS:
                return self.respond(404, {"error": "Plataforma não encontrada"})
            return self.respond(200, modules(platform_id))
        match = re.fullmatch(r"/modules/([0-9]+)/photos", request.path)
        if match:
            module_id = int(match[1])
            if not 1 <= module_id <= PLATFORMS * MODULES_PER_PLATFORM:
                return self.respond(404, {"error": "Módulo não encontrado"})
            query = parse_qs(request.query, keep_blank_values=True)
            try:
                def parameter(name, default):
                    values = query.get(name, [str(default)])
                    if len(values) != 1:
                        raise ValueError()
                    return int(values[0])
                offset = parameter("offset", 0)
                limit = parameter("limit", 50)
                if offset < 0 or not 1 <= limit <= MAX_LIMIT:
                    raise ValueError()
            except ValueError:
                return self.respond(400, {"error": "offset deve ser inteiro >= 0; limit deve ser inteiro entre 1 e 200"})
            return self.respond(200, photos(module_id, offset, limit))
        self.respond(404, {"error": "Endpoint não encontrado"})


if __name__ == "__main__":
    port = int(os.getenv("PORT", "8000"))
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"REST API disponível em http://localhost:{port}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
