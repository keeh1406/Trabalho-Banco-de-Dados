import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import br.edu.opet.util.Leitor;
import redis.clients.jedis.Jedis;

public class TelaPrimaria {

	private static DateTimeFormatter sFormatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static DateTimeFormatter sFormatadorDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy:HH:mm:ss");

	public static void main(String[] args) {
		// Conectando no Redis server - localhost
		Jedis jedis = new Jedis("localhost");

		loop: while (true) {

			System.out.println();
			System.out.println("-----Menu Principal-----");
			System.out.println("1 - Criar Novo Usuário");
			System.out.println("2 - Fazer Login");
			System.out.println("3 - Sair do Programa");
			System.out.println();

			int tMenuPrincipal = Leitor.readInt("O que você deseja fazer? ");
            // Início do Menu Principal
			loop2: switch (tMenuPrincipal) {

			case 1:

				LocalDate tDataAtualCadastro = LocalDate.now();
				// Pegando a Data Atual da efetuação do cadastro
				
                // Cadastro de Usuário Novo
				System.out.println("Novo Usuário");
				String tNomeUsuario = Leitor.readString("Nome do Usuário:");
				// Pede pro Usuário digitar o nome
				
				if (tNomeUsuario == "") {
					System.out.println("Nome do Usuário não pode estar vazio!");
					break loop2;
					// Se o Nome do Usuário for vazio, volta para o Menu Principal
				}
				while (true) {
					String tApelidoUsuario = Leitor.readString("Apelido:");
					if (tApelidoUsuario == "") {
						System.out.println("Apelido não pode estar vazio!");
						break loop2;
						// Se o Apelido do Usuário for vazio, volta para o início
					}

					String tApelidoBancoDados = jedis.hget("Usuario:" + tApelidoUsuario, "Apelido");
					// Procura no Banco de Dados se apelido escolhido pelo Usuário já está em uso

					if (!(tApelidoBancoDados == null)) {
						System.out.println("Este apelido de usuário já está em uso. Tente outro.");
						// No banco, se der é porque está nulo e já existe
						// Se o apelido já existir, ele dá a mensagem e pede para o usuário digitar novamente outra coisa
						System.out.println();
					} 
						jedis.hset("Usuario:" + tApelidoUsuario, "Nome", tNomeUsuario);
						// Guarda no Banco a informação do Nome do Usuário
						jedis.hset("Usuario:" + tApelidoUsuario, "Apelido", tApelidoUsuario);
						// Guarda no Banco a informação do Apelido do Usuário
						jedis.hset("Usuario:" + tApelidoUsuario, "DataCadastro", tDataAtualCadastro.format(sFormatador));
						// Guarda no Banco a informação da Data de Cadastro do Usuário
						break loop2;
						// Guardando no Banco as informações
				}

			case 2:

				while (true) {
					System.out.println();
					System.out.println("LOGIN");
					// Pede pro Usuário digitar o apelido para entrar no sistema
					String tApelidoUsuarioDigitado = Leitor.readString("Apelido:");
					if (tApelidoUsuarioDigitado == "") {
						break loop2;
						// Se o apelido digitado for vazio, ele volta para o Menu Principal
					}

					String tApelidoBancoDados = jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "Apelido");
					// Procura no Banco se o Apelido digitado existe

					if (tApelidoBancoDados == null || !tApelidoBancoDados.equals(tApelidoUsuarioDigitado)){
						// Se o apelido guardado no Banco de Dados for nulo e/ou o apelido digitano não for igual ao apelido gravado no Banco, dá erro
						System.out.println(" Apelido inválido");
					} else {
						while (true) {

							System.out.println();
							System.out.println("---Menu Secundário---");
							System.out.println("1 - Enviar Mensagem");
							System.out.println("2 - Caixa de Entradas");
							System.out.println("3 - Caixa de Saída");
							System.out.println("4 - Visualizar Dados");
							System.out.println("5 - Encerrar sessão");
							System.out.println();

							int tMenuSegundario = Leitor.readInt("O que você desejá fazer? ");
                            // Menu Secundário
							loop3: switch (tMenuSegundario) {

							case 1:
                                
								LocalDateTime tDataAtualMensagem = LocalDateTime.now();
								// Pegando a Data Atual e a Hora para enviar a mensagem
								
								System.out.println();
								String tPara = Leitor.readString("Para:");
								// Quem receberá a mensagem

								if (tPara == "") {
									System.out.println("O destinatário não pode estar vazio!");
									break loop3;
									// Se quem receber a mensagem for nulo, volta para o Menu Secundário
								}

								String tMensagem = Leitor.readString("Mensagem:");
								// Texto que será enviado 

								if (tMensagem == "") {
									System.out.println("A mensagem não pode estar vazia!");
									break loop3;
									// Se a mensagem for nula, volta para o menu Secundário
								}

								jedis.sadd(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora) + ":Para",tPara);
								// Guardando no banco a informação junto com a data e a hora para quem está sendo enviada a mensagem
								jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora) + ":De",tApelidoUsuarioDigitado);
								// Guardando no banco a informação junto com a data e a hora de quem está enviando a mensagem
								jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora) + ":Mensagem",tMensagem);
								// Guardando  no banco a informação junto com a data e a hora a mensagem enviada
								
								Long nSaidas = jedis.incr(tApelidoUsuarioDigitado + "--saida");
								// Fica incrementando, quantas mensagens foram enviadas, saídas
								jedis.zadd(tApelidoUsuarioDigitado + "--saida ", nSaidas,tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora));
								// Guarda no Banco em conjunto ordenado em chaves, quem enviou, tantas mensagem que sairam e a data da mensagem

								String[] textoSeparado = tPara.split(",");
								// Pode-se adicionar mais pessoas que receberão a mensagem
								for (int i = 0; i < textoSeparado.length; i++) {Long nEntradas = jedis.incr(textoSeparado[i] + "--entrada ");
                                    // Enquanto i for maior que 0, vai incrementando as mensagens vindas
									jedis.zadd(textoSeparado[i] + "--entrada  ", nEntradas, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora));
									// Guarda no Banco de Dados as mensagens que chegaram, quem enviou e a data e hora
								}
								break loop3;

							case 2:

								System.out.println("Caixa de Entrada");

								Long contadormensagens = jedis.zlexcount(tApelidoUsuarioDigitado + "--entrada  ", "-", "+");
								// Conta quantas mensagem o usuário recebeu
								System.out.println("Você tem " + contadormensagens + " mensagens.");
								// Mostra a conta de quantas mensagens o usuário recebeu

								if (contadormensagens == 0) {
									System.out.println("Você não tem nenhuma mensagem!");
                                    // Se o número de mensagens for zero, ele volta para o Menu Secundário
									break loop3;
								} else {
									for (int i = 0; i < contadormensagens; i++) { System.out.println((1 + i) + " " + jedis.zrange(tApelidoUsuarioDigitado + "--entr  ", i, i));
                                    // Mostra todas as mensagens que esse usuário recebeu em uma lista ordenada
									// Contando a partir de i até o total de mensagens que ele recebeu
									}

									System.out.println();
									Long tVisualisar = Leitor.readLong("Qual mensagem você quer visualizar?");

									if (tVisualisar == 0) {
										System.out.println("Não tem mensagem!");
										// Se a mensagem for 0, ela não existe
										break loop3;
									} else {

										Set<String> tMensagemVista = jedis.zrange(tApelidoUsuarioDigitado + "--entrada  ", tVisualisar - 1, tVisualisar - 1);
										// Mostra quem enviou e todas as mensagens que essa pessoa enviou

										String MensagemTransformada = (tMensagemVista).toString();
										// Transforma a tMensagemVista em uma String
										String tMensagemCortada = MensagemTransformada.substring(1, MensagemTransformada.length() - 1);
										// Tira o primeiro e o ultimo caractere para tirá-la da chave 

										System.out.println(
												jedis.get(tMensagemCortada + ":De") + ": " + jedis.get(tMensagemCortada + ":Mensagem"));
										        // Mostra quem enviou e a mensagem enviada
										System.out.println();

										String tResposta = Leitor.readString("Resposta:");

										if (tResposta == "") {
											System.out.println("A mensagem não pode ser vazia!");
											// A resposta se for vazia, retorna para o Menu Secundário
											break loop3;
										} else {
											LocalDateTime tDataAtualMensagem2 = LocalDateTime.now();
											// Pegando a Data Atual e a Hora para enviar a mensagem

											Long nEntradasResposta = jedis.incr(tMensagemCortada + "--resposta  ");
											// Mostra quantas respostas foram enviadas para aquela mensagem
											
											jedis.zadd(tMensagemCortada + "--resposta ", nEntradasResposta, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora));
											//
											String tPara2 = jedis.get(tMensagemCortada + ":De");
											// Pega a mensagem 

											jedis.sadd(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora) + ":Para",tPara2);
											// Guardando no banco a informação junto com a data e a hora para quem está sendo enviada a mensagem
											jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora) + ":De",tApelidoUsuarioDigitado);
											// Guardando no banco a informação junto com a data e a hora de quem está enviando a mensagem
											jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora) + ":Mensagem",tResposta);
											// Guardando  no banco a informação junto com a data e a hora a mensagem enviada
											
											Long nSaidas2 = jedis.incr(tApelidoUsuarioDigitado + "--saida");
											// Fica incrementando, quantas mensagens foram enviadas, saídas
											jedis.zadd(tApelidoUsuarioDigitado + "--saida ", nSaidas2, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora));
											// Guarda no Banco em conjunto ordenado em chaves, quem enviou, tantas mensagem que sairam e a data da mensagem
											
											Long nEntradas2 = jedis.incr(tPara2 + "--entrada ");
											// Fica incrementando, adicionando quem receberá

											jedis.zadd(tPara2 + "--entrada  ", nEntradas2, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora));
											// Guarda no Banco de Dados as mensagens que chegaram para tal pessoa, quem enviou e a data e hora
										}
									}
								}
								break loop3;

							case 3:

								System.out.println("Caixa de Saida");

								Long contadorsaidas = jedis.zlexcount(tApelidoUsuarioDigitado + "--saida ", "-", "+");
								// Conta quantas mensagens o usuário enviou 

								System.out.println("Você enviou " + contadorsaidas + " mensagens.");
								// Mostra quantas mensagens o usuário enviou

								if (contadorsaidas == 0) {
									System.out.println("Você não enviou nenhuma mensagem!"); 
									break loop3;
								} else {

									for (int i = 0; i < contadorsaidas; i++) {

										System.out.println((1 + i) + " " + jedis.zrange(tApelidoUsuarioDigitado + "--saida ", i, i));
										// Se i for maior que zero, mostra as mensagens enviadas
									}

									loop4: while (true) {

										System.out.println();
										Long tVisualisarSaida = Leitor.readLong("Qual mensagem você quer visualizar?");

										if (tVisualisarSaida == 0 || tVisualisarSaida < 0) {
											// Se o usuário digitar 0 ou que ela não exista, dá erro
											System.out.println("Essa mensagem não existe!");
											break loop3;
										} else {

											Set<String> tMensagemVista3 = jedis.zrange(tApelidoUsuarioDigitado + "--saida ", tVisualisarSaida - 1, tVisualisarSaida - 1);
											// Pega da Lista, as mensagens que aquela usuário enviou

											String MensagemTransformada = (tMensagemVista3).toString();
											// Transforma a tMensagemVista3 em ums String
											
											String tMensagemVista4 = MensagemTransformada.substring(1, MensagemTransformada.length() - 1);
											// Tira o primeiro e o ultimo caractere para tirá-la da chave

											System.out.println("Para: " + jedis.smembers(tMensagemVista4 + ":Para"));
											// Mostra o destinatário dessa mensagem
											
											System.out.println(jedis.get(tMensagemVista4 + ":Mensagem"));
											// Mostra a mensagem enviada

											Long contadorrespostas = jedis.zlexcount(tMensagemVista4 + "--respo ", "-", "+");
											// Conta quantas respostas o usuário enviou
											System.out.println();
											System.out.println("Você possui " + contadorrespostas + " respostas.");
											// Mostra quantas respostas ele enviou
											
											
											if (contadorrespostas == 0) {
												System.out.println("Você não tem nenhuma mensagem!");
												break loop4;
											} else {
												System.out.println("Respostas:");
												
												for (int i = 0; i < contadorrespostas; i++) {Set<String> tMensagemVista5 = jedis.zrange(tMensagemVista4 + "--resposta ", i, i);
												// Se o houver mensagens enviadas, pega da lista as respostas enviadas

													String stringTransforma2 = (tMensagemVista5).toString();
													// Pega a tMensagemVista5 e a transforma em uma String
													
													String tTirandochave = stringTransforma2.substring(1,stringTransforma2.length() - 1);
													// Tira o primeiro e o ultimo caractere para tirá-la da chave 

													String tNomeUsuario2 = jedis.get(tTirandochave + ":De");
													// Pega do banco quem enviou aquela resposta

													System.out.println((1+i) + "- " + tNomeUsuario2);
													// Mostra quem enviou aquela resposta
												}

												System.out.println();
												Long tVisualisarResposta = Leitor.readLong("Qual mensagem você quer visualizar?");

												if (tVisualisarResposta == 0 || tVisualisarResposta < 0) {
													System.out.println("Essa mensagem não existe!");
													break loop3;
												} else {

													Set<String> tMensagemVista7 = jedis.zrange(tMensagemVista4 + "--resposta ", tVisualisarResposta - 1, tVisualisarResposta - 1);
													// Se houver respostas enviadas, pega da lista
													
													String stringTransformada3 = (tMensagemVista7).toString();
													// Pega a tMensagemVista7 e a transforma em uma String
													
													String tTirandochave2 = stringTransformada3.substring(1, stringTransformada3.length() - 1);
													// Tira o primeiro e o ultimo caractere para tirá-la da chave 

													System.out.println(jedis.get(tTirandochave2 + ":Mensagem"));
													// Mostra a mensagem enviada

													String Opcao = Leitor.readString("Enter para Sair!");
													if (Opcao == "")
														System.out.println();
														System.out.println("Você saiu do Visualizador de Mensagens!");
														break loop4;
												}
											}
										}
									}
								}
								break loop3;

							case 4:
								System.out.println();
								System.out.println("Visualizar os Dados");

								System.out.println("Apelido: " + jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "Apelido"));
								// Mostra o apelido do usuário que está logado
								System.out.println("Nome: " + jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "Nome"));
								// Mostra o nome do usuário que está logado
								System.out.println("Data de Cadastro: " + jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "DataCadastro"));
								// Mostra a data do cadastro do usuário que está logado
								System.out.println();
								break loop3;

							case 5:
								System.out.println("Fim da Sessão!");
								break loop2;
							}
						}
					}
				}

			case 3:
				System.out.println("Fim do Programa!");
				break loop;
			}

		}
		jedis.close();
	}
}