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
			System.out.println("1 - Criar Novo Usuário");
			System.out.println("2 - Fazer Login");
			System.out.println("3 - Sair do Programa");

			System.out.println();

			int tMenuPrincipal = Leitor.readInt("O que você deseja fazer? ");

			loop2: switch (tMenuPrincipal) {

			case 1:

				LocalDate tDataAtualCadastro = LocalDate.now();

				System.out.println("Novo Usuário");
				String tNomeUsuario = Leitor.readString("Nome:");
				if (tNomeUsuario == "") {
					break loop2;
				}
				while (true) {

					String tApelidoUsuario = Leitor.readString("Apelido:");
					if (tApelidoUsuario == "") {
						break loop2;
					}

					String tApelidoBancoDados = jedis.hget("Usuario:" + tApelidoUsuario, "Apelido");

					if (!(tApelidoBancoDados == null)) {
						System.out.println("Este apelido de usuário já está em uso. Tente outro.");
						System.out.println();
					} 
						jedis.hset("Usuario:" + tApelidoUsuario, "Nome", tNomeUsuario);
						jedis.hset("Usuario:" + tApelidoUsuario, "Apelido", tApelidoUsuario);
						jedis.hset("Usuario:" + tApelidoUsuario, "DataCadastro", tDataAtualCadastro.format(sFormatador));
						break loop2;
				}

			case 2:

				while (true) {
					System.out.println();
					System.out.println("LOGIN");
					String tApelidoUsuarioDigitado = Leitor.readString("Apelido:");
					if (tApelidoUsuarioDigitado == "") {
						break loop2;
					}

					String tApelidoBancoDados = jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "Apelido");

					if (tApelidoBancoDados == null || !tApelidoBancoDados.equals(tApelidoUsuarioDigitado)){
						System.out.println(" Apelido inválido");
					} else {
						while (true) {

							System.out.println();
							System.out.println("1 - Enviar Mensagem");
							System.out.println("2 - Caixa de Entradas");
							System.out.println("3 - Caixa de Saída");
							System.out.println("4 - Visualizar Dados");
							System.out.println("5 - Encerrar sessão");
							System.out.println();

							int tMenuSegundario = Leitor.readInt("O que você desejá fazer? ");

							loop3: switch (tMenuSegundario) {

							case 1:

								LocalDateTime tDataAtualMensagem = LocalDateTime.now();

								System.out.println();
								String tPara = Leitor.readString("Para:");

								if (tPara == "") {
									break loop3;
								}

								String tMensagem = Leitor.readString("Mensagem:");

								if (tMensagem == "") {
									break loop3;
								}

								jedis.sadd(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora) + ":Para",tPara);
								jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora) + ":De",tApelidoUsuarioDigitado);
								jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora) + ":Mensagem",tMensagem);
								
								Long nSaidas = jedis.incr(tApelidoUsuarioDigitado + "--saida");
								jedis.zadd(tApelidoUsuarioDigitado + "--saida ", nSaidas,
										tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora));

								String[] textoSeparado = tPara.split(",");
								for (int i = 0; i < textoSeparado.length; i++) {Long nEntradas = jedis.incr(textoSeparado[i] + "--entr ");

									jedis.zadd(textoSeparado[i] + "--entr  ", nEntradas, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem.format(sFormatadorDataHora));
								}
								break loop3;

							case 2:

								System.out.println("Caixa de Entrada");

								Long contadorentradas = jedis.zlexcount(tApelidoUsuarioDigitado + "--entr  ", "-", "+");
								System.out.println("Você tem " + contadorentradas + " mensagens.");

								if (contadorentradas == 0) {

									break loop3;
								} else {
									for (int i = 0; i < contadorentradas; i++) { System.out.println((1 + i) + " " + jedis.zrange(tApelidoUsuarioDigitado + "--entr  ", i, i));

									}

									System.out.println();
									Long tVisualisar = Leitor.readLong("Visualisar mensagem:");

									if (tVisualisar == 0) {
										break loop3;
									} else {

										Set<String> tMensagemVista = jedis.zrange(tApelidoUsuarioDigitado + "--entr  ", tVisualisar - 1, tVisualisar - 1);

										String stringCortando = (tMensagemVista).toString();
										String tMensagemVista2 = stringCortando.substring(1, stringCortando.length() - 1);

										System.out.println(
												jedis.get(tMensagemVista2 + ":De") + ": " + jedis.get(tMensagemVista2 + ":Mensagem"));
										System.out.println();

										String tResposta = Leitor.readString("Resposta:");

										if (tResposta == "") {
											break loop3;
										} else {
											LocalDateTime tDataAtualMensagem2 = LocalDateTime.now();

											Long nEntradasResposta = jedis.incr(tMensagemVista2 + "--respo  ");
											jedis.zadd(tMensagemVista2 + "--respo ", nEntradasResposta, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora));
											String tPara2 = jedis.get(tMensagemVista2 + ":De");

											jedis.sadd(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora) + ":Para",tPara2);
											jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora) + ":De",tApelidoUsuarioDigitado);
											jedis.set(tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora) + ":Mensagem",tResposta);
											
											Long nSaidas2 = jedis.incr(tApelidoUsuarioDigitado + "--saida");
											jedis.zadd(tApelidoUsuarioDigitado + "--saida ", nSaidas2, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora));

											Long nEntradas2 = jedis.incr(tPara2 + "--entr ");

											jedis.zadd(tPara2 + "--entr  ", nEntradas2, tApelidoUsuarioDigitado + ":" + tDataAtualMensagem2.format(sFormatadorDataHora));

										}
									}
								}
								break loop3;

							case 3:

								System.out.println("Caixa de Saida");

								Long contadorsaidas = jedis.zlexcount(tApelidoUsuarioDigitado + "--saida ", "-", "+");

								System.out.println("Você enviou " + contadorsaidas + " mensagens.");

								if (contadorsaidas == 0) {

									break loop3;
								} else {

									for (int i = 0; i < contadorsaidas; i++) {

										System.out.println(
												(1 + i) + " " + jedis.zrange(tApelidoUsuarioDigitado + "--saida ", i, i));
									}

									loop4: while (true) {

										System.out.println();
										Long tVisualisarSaida = Leitor.readLong("Visualisar mensagem:");

										if (tVisualisarSaida == 0 || tVisualisarSaida < 0) {
											break loop3;
										} else {

											Set<String> tMensagemVista3 = jedis.zrange(tApelidoUsuarioDigitado + "--saida ",
													tVisualisarSaida - 1, tVisualisarSaida - 1);

											String stringCortando = (tMensagemVista3).toString();
											String tMensagemVista4 = stringCortando.substring(1, stringCortando.length() - 1);

											System.out.println("Para: " + jedis.smembers(tMensagemVista4 + ":Para"));
											System.out.println(jedis.get(tMensagemVista4 + ":Mensagem"));

											Long contadorrespostas = jedis.zlexcount(tMensagemVista4 + "--respo ", "-", "+");
											System.out.println();
											System.out.println("Você possui " + contadorrespostas + " respostas.");
											
											
											if (contadorrespostas == 0) {
												break loop4;
											} else {
												System.out.println("Respostas:");
												
												for (int i = 0; i < contadorrespostas; i++) {
													Set<String> tMensagemVista5 = jedis.zrange(tMensagemVista4 + "--respo ", i, i);

													String stringCortando2 = (tMensagemVista5).toString();
													String tTirandochave = stringCortando2.substring(1,
															stringCortando2.length() - 1);

													String tNomeUsuario2 = jedis.get(tTirandochave + ":De");

													System.out.println((1+i) + "- " + tNomeUsuario2);
												}

												System.out.println();
												Long tVisualisarResposta = Leitor.readLong("Visualisar mensagem:");

												if (tVisualisarResposta == 0 || tVisualisarResposta < 0) {
													break loop3;
												} else {

													Set<String> tMensagemVista7 = jedis.zrange(tMensagemVista4 + "--respo ",
															tVisualisarResposta - 1, tVisualisarResposta - 1);
													String stringCortando3 = (tMensagemVista7).toString();
													String tTirandochave2 = stringCortando3.substring(1,
															stringCortando3.length() - 1);

													System.out.println(jedis.get(tTirandochave2 + ":Mensagem"));

													String opf = Leitor.readString("Enter para Sair!");
													if (opf == "")
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
								System.out.println("Nome: " + jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "Nome"));
								System.out.println("Data de Cadastro: " + jedis.hget("Usuario:" + tApelidoUsuarioDigitado, "DataCadastro"));
								System.out.println();
								break loop3;

							case 5:
								break loop2;
							}
						}
					}
				}

			case 3:
				break loop;
			}

		}
		jedis.close();
	}
}