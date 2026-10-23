# Roteiro rápido — SmartLog DEMO

Site: https://smartlog-next-2026.netlify.app/login
APK: https://smartlog-next-2026.netlify.app/downloads/smartlog-arm64.apk
Acessos e senha: arquivo privado .local/ACESSOS_SMARTLOG_DEMO.md na raiz do workspace.

## Base pronta

Empresa isolada “SmartLog NEXT — DEMONSTRAÇÃO”: 1 gestor, 2 motoristas ativos, 1 motorista inativo, 3 clientes, 3 veículos, 4 rotas e 12 entregas.

No painel inicial: 12 entregas; 6 pendentes (inclui 1 atrasada), 3 em rota, 2 entregues, 1 cancelada; 2 alertas de entregas sem motorista. Esses números mudam conforme você testa.

## Teste em 10 passos

1. **Login e painel:** entre com admin. Confira os totais acima, navegação e listas.
2. **Filtros:** busque ENT-N001; filtre por status, cliente e motorista. Limpe os filtros para recuperar a lista.
3. **Tablet:** entre com motorista1. Deve ver apenas suas entregas ENT-N001, ENT-N002 e ENT-N003.
4. **GPS:** inicie a rota Paulista, permita localização e confira posição/horário no painel. Pause e retome. O marcador inicial da rota Centro é FICTÍCIO; apenas o GPS enviado pelo aparelho prova rastreamento real.
5. **Entrega integrada:** altere ENT-N001 para Em Rota e depois Entregue. Aguarde confirmação no app e confira o mesmo código no painel após aproximadamente 5 segundos.
6. **IA:** pergunte “Qual o status da ENT-N001?” no app e no painel. A resposta deve acompanhar o status salvo. Pergunte por um código inexistente: o assistente não deve inventar dados.
7. **Cliente:** saia do app e entre com cliente1. Deve ver suas entregas ENT-N001, ENT-N002, ENT-N007 e ENT-N011, sem poder alterar status.
8. **Cadastros:** no painel, crie um veículo, uma rota e uma entrega com peso/volumes/valor. Use cliente2 e motorista2. Edite os registros e confira persistência após recarregar. Para testar novos usuários, use e-mails seus de teste; a recuperação de senha precisa de caixa postal real.
9. **Perfil e acesso:** com motorista1, edite nome/telefone e entre novamente. A conta inativo deve ser impedida de consultar ou alterar a operação. Motorista/cliente não devem acessar a administração do painel.
10. **Falha de rede:** no tablet, desligue a internet e tente mudar o status de ENT-N002. Não deve confirmar uma gravação inexistente. Reconecte e confira o estado salvo antes de repetir.

Apague somente os registros extras que você criou para testar exclusão. Os e-mails mockados não recebem mensagens. Fotos, assinatura de comprovante, tráfego e previsão de chegada não são capacidades comprovadas por esta base.

## Reutilizar

Os dados ficam salvos no Firebase; não dependem do desktop. O script SmartLogAPI/netlify/seed-demo.mjs --apply cria somente o que falta e preserva suas alterações. Não reinicia os status nem troca senhas. Não inclua o arquivo privado de acessos no Git.
