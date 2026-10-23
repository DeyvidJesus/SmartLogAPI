# Correção de ícones e trajetos — 23/10/2026

## Causa
A main original do Lucas já estava integrada. A tela administrativa mapas.dart consulta Mapbox Directions; o mapa do motorista desenhava uma Polyline diretamente entre as coordenadas das entregas, sem roteamento viário. O APK anterior também não tinha token Mapbox configurado.

## Correção
- Painel: ícones SVG locais nos indicadores, menu e cabeçalho; marcador Leaflet em SVG, sem depender da descoberta automática de arquivos PNG.
- Motorista: escolha da rota exibida, paradas ordenadas dessa rota e geometria viária completa, incluindo o último GPS enviado como origem.
- Paradas sem coordenadas: geocodificação nativa Android/iOS, com cache.
- Falha de roteamento: mensagem e ausência da linha; nenhuma substituição por segmentos retos.
- Mapbox do Lucas preservado quando configurado. Sem token, a tela administrativa usa o mapa viário compartilhado.
- GPS continua vindo do aparelho. O traçado é um caminho calculado pelas ruas, não um histórico gravado do percurso nem navegação com instruções de voz.

## Provedor da demonstração
OSRM/FOSSGIS, gratuito e sem conta: https://routing.openstreetmap.de/about.html
Respeitar uso leve; sem garantia de disponibilidade ou trânsito ao vivo. O app identifica as requisições, mantém cache e espaça chamadas. Atualizações por movimento são limitadas a 30 segundos e deslocamento de 50 metros. ROUTING_URL permite usar outra implantação OSRM no build.

A geocodificação usa os serviços nativos do aparelho; depende de disponibilidade e conectividade. Não foi incorporado o Nominatim público.

## Verificação realizada
Três testes focados: geometria completa e cache; falha sem linha reta; rejeição de ponto único. Consulta real em São Paulo retornou 71 pontos de geometria e 2.274 metros. Build Angular aprovado; análise Flutter sem erros. Conferência visual no navegador e no tablet ainda depende do ensaio.

## Teste rápido
1. Atualize o painel com Ctrl+F5 e confira os ícones e o marcador no mapa.
2. Reinstale o APK atualizado, entre com motorista1 e abra o mapa de rotas.
3. Selecione Paulista, inicie o GPS e confira a linha acompanhando as ruas.
4. Se estiver longe das paradas fictícias em São Paulo, o trajeto começará na sua posição real e poderá ser longo.
5. Para conferir falha, desconecte a internet e solicite atualização: não deve aparecer uma linha reta simulada.

APK build 2: 116275251 bytes. SHA-256: 8b5438069a12a76e1c39340c9e176bd3ffa4d90605d43093b8fa7db199b879c1

Publicado no deploy 6adbae8e10392dbed31ebb7b. Bundle do painel e APK conferidos contra os arquivos servidos pelo Netlify.
