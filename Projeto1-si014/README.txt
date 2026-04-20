Projeto 1
Grupo - si014
Bruno Subtil - 62249
José Lourenço - 62817
Tomás Rodrigues - 60932
---------------------------------


// Criar keystore (Comandos que utilizamos para criar as keystores exportar e importar)
"C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\keytool.exe" -genkeypair -keysize 2048 -keyalg RSA -keystore keystore.Maria -alias Maria -storetype PKCS12
O caminho do keytool.exe pode ser diferente, no meu computador estava aqui.

// Exportar importar cert

keytool -exportcert -alias Maria -keystore keystore.Maria -file maria.cer

keytool -importcert -alias Maria -file maria.cer -keystore keystore.Bruno

Já se encontram duas keystores criadas na pasta KeyStores para testes, ambas têm os certificados necessários para efetuar operações entre os dois utilizadores.
Nomeadamente a keystore Maria e Bruno.

a password de ambas é 123456.

------------------------------------------------------------------------------------------------

// Como executar (Abrir terminal na pasta onde se encontram os ficheiros .java, um para o servidor e n para clientes, pasta servidor e cliente)

- Ficheiro test.txt (pasta cliente)
Existe um ficheiro test.txt pode ser usado para testar as operações, ao decifrar e validar localmente tem de usar o nome inteiro do ficheiro (ex: test.txt.cifrado)

- Compilar ficheiros java 
javac *.java nas pastas do servidor e cliente (deixá-mos já compilado, se necessário compilar novamente).

- Ligar o servidor (executar dentro da pasta do servidor)
java mySaudeServer <porta> (ex:4444)

- Diretorias do servidor
O servidor vem com 2 diretorias criadas já, nomeadamente Maria e Bruno prontas para testes, podem-se criar mais conforme necessário. 

- KeyStores
A KeyStore tem de estar ao mesmo nível do cliente para o mesmo a poder ler (dentro da pasta do cliente, deixámos cópias dentro da pasta cliente das que estão na pasta KeyStore).

------------
| Comandos | (executar dentro da pasta do cliente, a pass é 123456 para a maria e para o bruno)
------------

- Enviar e receber ficheiros do cliente para o servidor
java mySaude -s endereço:porto_do_servidor -u username -e nomes_de_ficheiros -t username_do_destinatario
java mySaude -s endereço:porto_do_servidor -u username -r nomes_de_ficheiros

- Cifrar/decifrar ficheiros
java mySaude -u username -p password -c nomes_de_ficheiros -t username_do_destinatario
java mySaude -u username -p password -d nomes_de_ficheiros

- Cifrar/decifrar ficheiros e enviar/receber
java mySaude -s endereço:porto_do_servidor -u username -p password -ce nomes_de_ficheiros -t username_do_destinatario
java mySaude -s endereço:porto_do_servidor -u username -p password -rd nomes_de_ficheiros

- Assinar/validar a assinatura de ficheiros
java mySaude -u username -p password -a nomes_de_ficheiros
java mySaude -u username -p password -v nomes_de_ficheiros -t username_de_quem_assinou

- Assinar/validar a assinatura de ficheiros e enviar/receber os ficheiros e assinaturas
java mySaude -s endereço:porto_do_servidor -u username -p password -ae nomes_de_ficheiros -t username_do_destinatario
java mySaude -s endereço:porto_do_servidor -u username -p password -rv nomes_de_ficheiros -t username_de_quem_assinou

- Assinar, cifra e envia / recebe, decifra e verifica assinatura (envelope seguro)
java mySaude -s endereço:porto_do_servidor -u username -p password -ace nomes_de_ficheiros -t username_do_destinatario
java mySaude -s endereço:porto_do_servidor -u username -p password -rdv nomes_de_ficheiros -t username_de_quem_assinou






