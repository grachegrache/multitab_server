package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import data.Inst;
import data.Modify;
import data.Program;
import data.Symbol;
import data.Token;

public class MyAssembler{
	private int					codeIndex;
	private ArrayList<ArrayList<String>>	extdef;
	private ArrayList<ArrayList<String>>	extref;
	private ArrayList<String>			inputData;
	private String				inputFile;
	private ArrayList<Inst>			inst;
	private int					labelNum;
	private static int					lastCtr;
	private static int locctr;
	private int					len;
	private static int					lineNum;
	private static int					mIndex;
	private ArrayList<ArrayList<Modify>>			modifyTable;
	private ArrayList<ArrayList<String>>			objectCode;
	private static String				outputFile;
	private ArrayList<Program> programInfo;
	private static ArrayList<Symbol>			symTable;
	private static ArrayList<Token>				tokenTable;
	
	public MyAssembler(){
		int maxLines = Constant.MAX_LINES;
		
		extdef = new ArrayList<ArrayList<String>>();
		extref = new ArrayList<ArrayList<String>>();
		programInfo = new ArrayList<Program>();
		
		inputData	= new ArrayList<String>(maxLines);
		inst		= new ArrayList<Inst>(Constant.MAX_INST);
		modifyTable		= new ArrayList<ArrayList<Modify>>();
		objectCode	= new ArrayList<ArrayList<String>>();
		symTable	= new ArrayList<Symbol>(maxLines);
		tokenTable	= new ArrayList<Token>(maxLines);
	}
	
	/* -----------------------------------------------------------------------------------
	 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다. 
	 * 매계 : 없음
	 * 반환 : 정상종료 = 0 , 에러 발생 = -1
	 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기 
	 *		   위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
	 *		   구현하였다. 
	 * -----------------------------------------------------------------------------------
	 */

	public int initMyAssembler(){
		int result;

		if((result = initInstFile("inst.data")) < 0)
			return -1;
		if((result = initInputFile()) < 0)
			return -1;
		return result;
	}

	/* -----------------------------------------------------------------------------------
	 * 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다. 
	 *		   패스1에서는..
	 *		   1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
	 *		   테이블을 생성한다. 
	 * 
	 * 매계 : 없음
	 * 반환 : 정상 종료 = 0 , 에러 = < 0 
	 * 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
	 *	  따라서 에러에 대한 검사 루틴을 추가해야 한다. 
	 *		
	 * -----------------------------------------------------------------------------------
	 */

	public static int assemPass1(){
		//input_data를 기반으로 token_table에 파싱
		int i = 0;
		for(; i < lineNum; i++)
			if(tokenParsing() < 0)
				return -1;
		printIntercode();
		return 0;
	}

	/* -----------------------------------------------------------------------------------
	 * 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다. 
	 *		   패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다. 
	 *		   다음과 같은 작업이 수행되어 진다. 
	 *		   1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다. 
	 * 매계 : 없음
	 * 반환 : 정상종료 = 0, 에러발생 = < 0 
	 * 주의 : 
	 * -----------------------------------------------------------------------------------
	 */

	public static int assemPass2(){
		int i;
		Integer k;
		locctr = 0;
		lastCtr = 0;
		mIndex = 0;
		k = -1;
		
		for(i = 0; i < lineNum; i++)
			makeIntercode(k);

		makeObjectcode();
		return 0;

	}
	/* -----------------------------------------------------------------------------------
	 * 설명 : 머신을 위한 기계 코드목록 파일을 읽어 기계어 목록 테이블(inst_table)을 
	 *        생성하는 함수이다. 
	 * 매계 : 기계어 목록 파일
	 * 반환 : 정상종료 = 0 , 에러 < 0 
	 * 주의 : 기계어 목록파일 형식은 다음과 같다. 
	 *	
	 *	===============================================================================
	 *		   | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | NULL|
	 *	===============================================================================	   
	 *		
	 * -----------------------------------------------------------------------------------
	 */

	private int initInstFile(String instFile){
		String instLine;
		StringTokenizer st;
		Inst inst;
		File file = new File(instFile);
				
		//파일 확인
		if((file.exists() && file.canRead()) == false)
			return -1;
		Scanner sc = new Scanner(file);
		
		try{
			//한 줄씩 입력받아 파싱
			while(sc.hasNextLine()){
				instLine = sc.nextLine();
				st = new StringTokenizer(instLine,"|");
				
				try{
					inst = new Inst();
					inst.name = st.nextToken();
					inst.format = st.nextToken();
					inst.opcode = st.nextToken();
					inst.operandSize = Integer.valueOf(st.nextToken());
				}catch(Exception e){
					e.printStackTrace();
					return -1;
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}finally{
			sc.close();
		}
		
		return 0;
	}

	/* -----------------------------------------------------------------------------------
	 * 설명 : 어셈블리 할 소스코드를 읽어오는 함수이다. 
	 * 매계 : 어셈블리할 소스파일명
	 * 반환 : 정상종료 = 0 , 에러 < 0  
	 * 주의 : 
	 *		
	 * -----------------------------------------------------------------------------------
	 */

	private int initInputFile(){
		String inputLine;
		File file = new File(inputFile);
		lineNum = 0;
		
		//파일 확인
		if((file.exists() && file.canRead()) == false)
			return -1;
		Scanner sc = new Scanner(file);
		
		try{
			//주석 . 은 제외하고 한 줄 씩 입력받음
			while(sc.hasNextLine()){
				inputLine = sc.nextLine();
				if(inputLine.trim().charAt(0) != '.'){
					inputData.add(inputLine);
					lineNum++;
					
				}
			}
		
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}finally{
			sc.close();
		}
		
		return 0;
	}

	/* -----------------------------------------------------------------------------------
	 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다. 
	 *        패스 1로 부터 호출된다. 
	 * 매계 : 소스코드의 라인번호  
	 * 반환 : 정상종료 = 0 , 에러 < 0 
	 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다. 
	 * -----------------------------------------------------------------------------------
	 */
	private int tokenParsing(String input){
		//변수 초기화
		int tabCount;
		tabCount = 0;
		String str;
		Token tok = new Token();
		StringTokenizer st = new StringTokenizer(input, "\t");
		StringTokenizer opSt;
		
		try{
			//소스 줄 끝까지 탭문자를 기준으로 파싱
			tok.label = st.nextToken();
			tok.operator = st.nextToken();
			
			opSt = new StringTokenizer(st.nextToken(), ",");
			while(opSt.hasMoreTokens())
				tok.operand.add(opSt.nextToken());
			
			if(st.hasMoreTokens())
				tok.comment = st.nextToken();
			
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		
		tokenTable.add(tok);
		
//		for(; i < strlen(input_data[index]); i++)
//			if(tab_count == 0 || tab_count == 1){
//				//label, operator의 경우
//				if(input_data[index][i] == '\t'){
//					char* dest = strndup(&input_data[index][last_index], i - last_index);
//
//					//첫 파싱인 경우
//					if(tab_count == 0)
//						//토큰이 START, END, OPCODE 셋 중 하나이면 operator로 저장함
//						//또는, 첫 문자가 +, #, @이거나 토큰이 EXTDEF, EXTREF, LTORG이면 operator로 저장함
//						if(search_opcode(dest) != -1 || ((strcmp(dest, "START") && strcmp(dest, "END") && strcmp(
//								dest, "EXTDEF") && strcmp(dest, "EXTREF")) == 0) || dest[0] == '+'){
//							line->label = "";
//							tab_count = 1;
//						}
//
//					switch(tab_count){
//						case 0:
//							line->label = dest;
//							break;
//						case 1:
//							line->operator = dest;
//							break;
//						default:
//							break;
//					}
//					tab_count++;
//					last_index = i + 1;
//				}
//			}else if(tab_count == 2){
//				//operand의 경우
//				if(input_data[index][i] == '\t' || input_data[index][i] == ','){
//					line->operand[operand_index++] = strndup(&input_data[index][last_index],
//							i - last_index);
//					last_index = i + 1;
//
//					if(input_data[index][i] == '\t'){
//						if(input_data[index][last_index] == '\t'){
//							//첫 번째 탭 문자 다음 문자가 탭 문자이면 건너뜀
//							last_index++;
//							i++;
//						}
//						tab_count++;
//					}
//				}
//			}else if(tab_count > 3)
//				return -1;
//
//		if(i != last_index)
//			//끝에 파싱하지 않은 토큰이 있으면
//			if(tab_count == 0){
//				//LTORG와 같은 하나의 명령어
//				line->operator = strndup(&input_data[index][last_index], i - last_index);
//
//			}else if(tab_count == 1){
//				//operator가 저장되지 않았음
//				line->operator = strndup(&input_data[index][last_index], i - last_index);
//
//			}else if(tab_count == 2){
//				//마지막 operand가 저장되지 않았음
//				line->operand[operand_index] = strndup(&input_data[index][last_index], i - last_index);
//
//			}else if(tab_count == 3){
//				//operand 다음에 탭 문자가 존재 => 주석이 존재함
//				line->comment = strndup(&input_data[index][last_index], i - last_index);
//
//			}
//		token_table[index] = line;

		return 0;
	}
	/* -----------------------------------------------------------------------------------
	 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다. 
	 * 매계 : 토큰 단위로 구분된 문자열 
	 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0 
	 * 주의 : 
	 *		
	 * -----------------------------------------------------------------------------------
	 */

	int searchOpcode(String str){
	//입력이 NULL이면 에러 반환
		if(str == null || str == "")
			return -1;

		try{
	//OPCODE 탐색
		for(Inst tmp :inst)
			if(tmp.name.equals(str))
				return inst.indexOf(tmp);
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		
	}

	/* -----------------------------------------------------------------------------------
	 * 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다. 
	 * 매계 : 생성할 오브젝트 파일명 
	 * 반환 : 없음
	 * 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어 
	 *        화면에 출력해준다. 
	 *		
	 * -----------------------------------------------------------------------------------
	 */

	 private void makeObjectcode(String fileName){
	//변수 초기화
		int j, extIndex, lastAddr, TSize, start;
		boolean isEnd;
		Program prog;

		//입력 값이 null이 아니면 파일로 출력
		if(fileName != null){
			try{
				File file = new File(outputFile);
				file.createNewFile();
				PrintStream ps = new PrintStream(file);
				System.setOut(ps);
			}catch(Exception e){
				e.printStackTrace();
				return;
			}finally{
				System.setOut(System.out);
			}
		}
		
		//생성한 object code로 실제 object code를 생성, 출력
		for(int i=0;i<programInfo.size();i++){
			isEnd = false;
			start = 0;
			lastAddr = 0;
			TSize = 0;
			
			//H 레코드 출력
			prog = programInfo.get(i);
			System.out.println(String.format("H%-6s000000%06X",prog.name,prog.length));
			
			//D 레코드 출력
			if(extdef.size() > 0 && extdef.get(i).size() > 0){
				System.out.print("D");
				for(String def : extdef.get(i)){
					extIndex = searchSymbol(def);
					System.out.print(String.format("%-6s%06X", def,symTable.get(extIndex).addr));
				}
				System.out.println();
			}
			
			//R 레코드 출력
			if(extref.size() > 0 && extref.get(i).size() > 0){
				System.out.print("R");
				for(String ref : extref.get(i))
					System.out.print(String.format("%-6s", ref));
				System.out.println();
			}
			
			//T 레코드 출력
			for(String code : objectCode.get(i)){
				if(code != null){
					if(TSize + code.length() > 60){
						j = objectCode.get(i).indexOf(code);
						
						System.out.print(String.format("T%06X%02X",lastAddr,TSize/2));
						for(int l = start;l<j;l++)
							System.out.print(objectCode.get(i).get(l));
						System.out.println();
						
						lastAddr += TSize / 2;
						start = j;
						TSize = 0;
					}
					
					TSize += code.length();
				}
			}
			
			//마지막 부분 출력
			if(start != j){
				System.out.print(String.format("T%06X%02X",lastAddr,TSize/2));
				for(int l = start;l<objectCode.size();l++)
					System.out.print(objectCode.get(i).get(l));
				System.out.println();
				
			}
			
			//M 레코드 출력
			for(Modify modify : modifyTable.get(i))
				System.out.println(String.format("M%06X%02X%c%s",modify.addr,modify.len,modify.op,modify.ref));
			
			//E 레코드 출력
			System.out.print("E");
			if(prog.name == "START")
				System.out.print("000000");
			System.out.println();
			System.out.println();
		}
		
		System.setOut(System.out);
	}

	/*
	 * token 하나로 object code를 만드는 함수 
	 */
	private void makeIntercode(Token token){
		int i, opcodeIndex, symbolIndex, format, TA, immediate, regNum, isExist, num;
		String hex, operator, operand, start;
		immediate = 0;
		isExist = 0;
		Program prog;
		
		format = getInstSize(token);
		if(format == 0){
			len = 0;
			lastCtr = 0;
			locctr = 0;
			object_code[code_index++] = 0;
			object_code[code_index++] = 0;
		}else if(format > 0){
			locctr += format;
		}

	//opcode 검색
		operator = token->operator;
		if(token->operator[0] == '+')
			operator += 1;

	//opcode가 맞으면
		if((opcode_index = search_opcode(operator)) >= 0){

			//형식에 따라 동적할당
			switch(format){
				case 1:
					object_code[code_index] = calloc(3, sizeof(char));
					break;
				case 2:
					object_code[code_index] = calloc(5, sizeof(char));
					break;
				case 3:
					object_code[code_index] = calloc(7, sizeof(char));
					break;
				case 4:
					object_code[code_index] = calloc(9, sizeof(char));
					break;
				default:
					break;
			}

			if(format > 1)
				strcpy(object_code[code_index], inst[opcode_index][2]);

			if(format > 2){
				object_code[code_index][2] = '0';
				object_code[code_index][3] = 0;
			}else if(format == 2)
				object_code[code_index][2] = 0;

			if(format > 2){
				//#, @(immediate, indirect) 검색
				operand = token->operand[0];
				num = get_dec(object_code[code_index][1]);

				switch(operand[0]){
					case '#':
						num += I;
						operand += 1;
						immediate = 1;
						break;
					case '@':
						num += N;
						operand += 1;
						break;
					default:
						num += N + I;
						break;
				}
				object_code[code_index][1] = get_hex(num);

				//x, e 찾기
				for(i = 1; i < 3; i++)
					if(token->operand[i] != NULL)
						if(strcmp(token->operand[i], "X") == 0){
							is_exist = 1;
							break;
						}

				if(is_exist){
					num = get_dec(object_code[code_index][2]);
					num += op_X;
					object_code[code_index][2] = get_hex(num);
				}
			}
			if(format == 4){
				num = get_dec(object_code[code_index][2]);
				num += E;
				object_code[code_index][2] = get_hex(num);
			}

			//TA 검색
			if(format > 2){
				if(strcmp(operand, "") == 0){
					TA = -1;
					hex = "000";
				}else if(is_digit(operand)){
					if(immediate)
						hex = get_formated_hex(dectohex(atoi(operand)), 3);
					else
						TA = atoi(operand);
				}else if((symbol_index = search_extref(operand, k)) != -1){
					TA = -1;
					hex = "000";
				}else if((symbol_index = search_symbol(operand)) != -1){
					TA = sym_table[symbol_index].addr;
				}else
					TA = -1;
			}

			//주소 계산
			if(immediate == 0){
				switch(format){
					case 2:
						for(i = 0; i < 2; i++){
							if((reg_num = get_reg_num(token->operand[i])) != -1)
								hex = dectohex(reg_num);
							else
								hex = "0";
							strcat(object_code[code_index], hex);
						}
						break;
					case 3:
						if(TA != -1){
							if(strcmp(operator, "RSUB") != 0){
								if(TA - locctr >= 16 * 16 * 16){
									num = get_dec(object_code[code_index][2]);
									num += op_B;
									object_code[code_index][2] = get_hex(num);

								}else{
									num = get_dec(object_code[code_index][2]);
									num += P;
									object_code[code_index][2] = get_hex(num);

									hex = get_formated_hex(dectohex(TA - locctr), 3);
									strcat(object_code[code_index], hex);
								}
							}
						}else if(strcmp(hex, "000") == 0)
							strcat(object_code[code_index], hex);
						break;
					case 4:
						hex = "00000";
						m_table[m_index].program_index = k;
						m_table[m_index].len = 5;
						m_table[m_index].addr = len / 2 + 1;
						m_table[m_index].op = '+';
						m_table[m_index].ref = token->operand[0];
						m_index++;
						strcat(object_code[code_index], hex);
						break;
					default:
						break;
				}
			}else if(hex != 0)
				strcat(object_code[code_index], hex);

		}else if((strcmp(token->operator, "WORD") && strcmp(token->operator, "BYTE")) == 0){
			//opcode가 아니고 WORD 혹은 BYTE일 때
			if(strcmp(token->operator, "WORD") == 0){
				//숫자로 이루어져 있지 않으면
				if(!is_digit(token->operand[0])){
					char op;
					int index_op, index_op1, index_op2;
					char* op1;
					char* op2;

					//연산자 찾기
					if(strstr(token->operand[0], "+") != NULL){
						op = '+';
					}else if(strstr(token->operand[0], "-") != NULL){
						op = '-';
					}else if(strstr(token->operand[0], "*") != NULL){
						op = '*';
					}else if(strstr(token->operand[0], "/") != NULL){
						op = '/';
					}

					//연산자 인덱스 찾기
					for(index_op = 0; index_op < strlen(token->operand[0]); index_op++)
						if(token->operand[0][index_op] == op)
							break;

					//피연산자 복사
					op1 = strndup(token->operand[0], index_op);
					op2 = strdup(token->operand[0] + index_op + 1);

					m_table[m_index].program_index = k;
					m_table[m_index].len = 6;
					m_table[m_index].addr = len / 2;
					m_table[m_index].op = '+';
					m_table[m_index].ref = op1;
					m_index++;
					m_table[m_index].program_index = k;
					m_table[m_index].len = 6;
					m_table[m_index].addr = len / 2;
					m_table[m_index].op = op;
					m_table[m_index].ref = op2;
					m_index++;

					hex = "000000";
				}else
					hex = get_formated_hex(dectohex(atoi(token->operand[0])), 6);

			}else if(strcmp(token->operator, "BYTE") == 0){
				start = strstr(token->operand[0], "'") + 1;

				for(i = 0; i < strlen(start); i++)
					if(start[i] == '\'')
						break;

				switch(token->operand[0][0]){
					case 'X':
						hex = strndup(start, i);
						break;
					case 'C':
						hex = calloc(i * 2, sizeof(char));
						hex[0] = 0;
						for(i = 0; i < strlen(start) - 1; i++)
							strcat(hex, dectohex(start[i]));
						break;
					default:
						break;
				}

			}

			object_code[code_index] = hex;

		}

		if(object_code[code_index] != NULL){
			len += strlen(object_code[code_index]);
			//전 줄에서 object code가 파싱되지 않았을 때
			if(locctr - last_ctr != format){
				object_code[code_index + 1] = object_code[code_index];
				object_code[code_index] = 0;
				code_index++;
			}
			last_ctr = locctr;
			code_index++;
		}

	//LTORG나 END를 만나면 object_code에 리터럴 추가
		if((strcmp(token->operator, "LTORG") && strcmp(token->operator, "END")) == 0)
			while((len = add_literal()) > 0)
				;

	}

	/*
	 * 중간코드를 콘솔 창에 출력하며
	 * sym_table에 심볼을 저장하고
	 * LTORG, END를 만나면 리터럴을 추가하는 함수
	 * 
	 */
	 private void printIntercode(){
	//콘솔 창에 출력
		int i, j, instSize, index, opcodeIndex;
		String hex;
		index = 0;

		for(Token token : tokenTable){
			if((instSize = getInstSize(token)) != -1){
				if(instSize == 0){
					//program 객체 생성
					if(programInfo.isEmpty())
						programInfo.add(new Program());
					else{
						if(programInfo.size() != 1)
							programInfo.add(new Program());
						programInfo.get(programInfo.size()-1).length = locctr;
					}
					
					//extdef, extref
					extdef.add(new ArrayList<String>());
					extref.add(new ArrayList<String>());
					locctr = 0;
				}
			}
		}
		for(i = 0; i < line_num; i++){
			token = token_table[i];

			if((instSize = get_inst_size(token)) != -1){
				if(instSize == 0){
					k++;
					if(k > 0){
						program_length[k - 1] = locctr;
						def_index[k] = def_index[k - 1];
						ref_index[k] = ref_index[k - 1];
					}
					locctr = 0;
				}

				hex = get_formated_hex(dectohex(locctr), 4);
				printf("%s", hex);

			}

			//sym_table에 심볼 추가
			if(token->label != 0){
				if(search_symbol(token->label) == -1){
					if(strcmp(token->operator, "EQU") == 0){
						if(print_equal(token, &sym_table[index]))
							index++;

					}else{
						sym_table[index].symbol[0] = 0;
						strcpy(sym_table[index].symbol, token->label);
						sym_table[index++].addr = locctr;
					}
				}
			}

			if(inst_size > 0)
				locctr += inst_size;

			printf("\t");

			if(token->label != NULL)
				printf("%s", token->label);

			printf("\t%s\t", token->operator);

			//EXTDEF, EXTREF 저장
			if(def_index[k] < MAX_LINES)
				if(strcmp(token->operator, "EXTDEF") == 0)
					for(j = 0; j < MAX_OPERAND; j++){
						if(token->operand[j] != NULL)
							extdef[def_index[k]++] = token->operand[j];
					}
			if(ref_index[k] < MAX_LINES)
				if(strcmp(token->operator, "EXTREF") == 0){
					for(j = 0; j < MAX_OPERAND; j++){
						if(token->operand[j] != NULL)
							extref[ref_index[k]++] = token->operand[j];
					}
				}

			//리터럴 심볼 테이블에 추가
			if(token->operand[0] != NULL)
				if(token->operand[0][0] == '=')
					if(search_symbol(token->operand[0]) == -1){
						sym_table[index].symbol[0] = 0;
						strcpy(sym_table[index].symbol, token->operand[0]);
						sym_table[index++].addr = -1;

					}

			for(j = 0; j < MAX_OPERAND; j++){
				if(token->operand[j] != 0){
					if(j != 0){
						//첫 operand가 아니면
						printf(",");
					}
					printf("%s", token->operand[j]);
				}else
					break;
			}

			printf("\r\n");

			//LTORG 혹은 END를 만나면 이 때까지 만난 리터럴을 추가함
			if((strcmp(token->operator, "LTORG") && strcmp(token->operator, "END")) == 0)
				print_literal();
		}

		program_length[k] = locctr;
	}

	 print_literal(){
		int i;
		char* hex;

		for(i = 0; i < MAX_LINES; i++)
			if(sym_table[i].symbol[0] == '=' && sym_table[i].addr == -1){
				sym_table[i].addr = locctr;
				hex = get_formated_hex(dectohex(locctr), 4);
				printf("%s", hex);
				printf("\t*\t%s\r\n", sym_table[i].symbol);
				locctr += get_byte_size(sym_table[i].symbol + 1);
			}
	}

	int print_equal(token* token, symbol* symbol){
		char* hex;
		char* op1;
		char* op2;
		char op = 0;
		int i, index_op, index_op1, index_op2, result;
		result = -1;

		if(strcmp(token->operand[0], "*") == 0){
			result = locctr;

			//symbol table에 추가
			symbol->symbol[0] = 0;
			strcpy(symbol->symbol, token->label);

			symbol->addr = result;
		}else{
			//연산자 찾기
			if(strstr(token->operand[0], "+") != NULL){
				op = '+';
			}else if(strstr(token->operand[0], "-") != NULL){
				op = '-';
			}else if(strstr(token->operand[0], "*") != NULL){
				op = '*';
			}else if(strstr(token->operand[0], "/") != NULL){
				op = '/';
			}

			if(op == 0)
				return 0;

			//연산자 인덱스 찾기
			for(index_op = 0; index_op < strlen(token->operand[0]); index_op++)
				if(token->operand[0][index_op] == op)
					break;

			//피연산자 복사
			op1 = strndup(token->operand[0], index_op);
			op2 = strdup(token->operand[0] + index_op + 1);

			//피연산자 인덱스 찾기
			index_op1 = search_symbol(op1);
			index_op2 = search_symbol(op2);

			free(op1);
			free(op2);

			if(index_op1 == -1 || index_op2 == -1)
				return 0;

			//계산
			switch(op){
				case '+':
					result = sym_table[index_op1].addr + sym_table[index_op2].addr;
					break;
				case '-':
					result = sym_table[index_op1].addr - sym_table[index_op2].addr;
					break;
				case '*':
					result = sym_table[index_op1].addr * sym_table[index_op2].addr;
					break;
				case '/':
					result = sym_table[index_op1].addr / sym_table[index_op2].addr;
					break;
				default:
					return 0;
			}

		}

	//결과값이 찾아졌으면
		if(result != -1){

			//출력
			hex = get_formated_hex(dectohex(result), 4);
			printf("%s", hex);

			return 1;
		}

		return 0;
	}

	/*
	 * 현재 locctr값과 sym_table의 addr를 비교하여
	 * 같은 값을 갖는 리터럴을 object_code에 추가하는 함수
	 */
	int add_literal(){
		int i, size;
		char* code = NULL;

		for(i = 0; i < MAX_LINES; i++)
			if(locctr == sym_table[i].addr)
				if(sym_table[i].symbol[0] == '='){
					char* operand = sym_table[i].symbol + 1;
					char* start;
					start = strstr(operand, "'") + 1;

					for(i = 0; i < strlen(start); i++)
						if(start[i] == '\'')
							break;

					switch(operand[0]){
						case 'X':
							code = strndup(start, i);
							size = i / 2 + i % 2;
							break;
						case 'C':
							code = calloc(i * 2, sizeof(char));
							size = i;

							for(i = 0; i < strlen(start); i++)
								if(start[i] == '\'')
									break;
								else
									strcat(code, dectohex(start[i]));
							break;
						default:
							code = dectohex(atoi(operand[0]));
							i = strlen(code);
							size = i / 2 + i % 2;
							break;
					}

					object_code[code_index] = code;
					locctr += size;
					//전 줄에서 object code가 파싱되지 않았을 때
					if(locctr - last_ctr != size){
						object_code[code_index + 1] = object_code[code_index];
						object_code[code_index] = 0;
						code_index++;
					}
					code_index++;
					return strlen(code);
				}

		return 0;
	}

	/*
	 * symbol_table에서 같은 이름의 symbol이 존재하면 index 리턴
	 */
	int search_symbol(char* symbol){
		int i;

		if(symbol == NULL)
			return -1;

		for(i = 0; i < MAX_LINES; i++){
			if(strcmp(sym_table[i].symbol, symbol) == 0)
				return i;
		}

		return -1;
	}

	/*
	 * extref 에서 같은 이름의 인덱스 반환
	 */
	int search_extref(char* str, int program_index){
		int i, start;

		if(str == NULL)
			return -1;

		if(program_index > 0)
			start = ref_index[program_index - 1];

		for(i = start; i < ref_index[program_index]; i++){
			if(extref[i] != NULL)
				if(strcmp(extref[i], str) == 0)
					return i;
		}

		return -1;
	}

	int get_reg_num(char* operand){
		if(operand == NULL)
			return -1;

		switch(operand[0]){
			case 'A':
				return A;
				break;
			case 'B':
				return reg_B;
				break;
			case 'T':
				return T;
				break;
			case 'S':
				return S;
				break;
			case 'L':
				return L;
				break;
			case 'X':
				return reg_X;
				break;
			default:
				return -1;
				break;
		}
	}

	/*
	 * 파싱된 토큰 하나(instruction)의 size를 리턴
	 */
	int get_inst_size(token* token){
		int i, index;
		char* operator = token->operator;
		char* start = 0;

		if(operator == NULL)
			return -1;

		if(operator[0] == '+')
			index = search_opcode(&operator[1]);
		else
			index = search_opcode(operator);

		if(index != -1){
			if(strstr(inst[index][1], "/") != NULL){
				//형식에 / 문자가 존재 => 3 또는 4
				if(operator[0] == '+')
					return 4;
				else
					return 3;
			}else
				return atoi(inst[index][1]);
		}else{
			if((strcmp(operator, "START") && strcmp(operator, "CSECT")) == 0){
				return 0;

			}else if((strcmp(operator, "END") && strcmp(operator, "LTORG") && strcmp(operator, "EXTDEF") && strcmp(
					operator, "EXTREF")) == 0){
				return -1;

			}else if(strcmp(operator, "RESB") == 0){
				return atoi(token->operand[0]);

			}else if(strcmp(operator, "RESW") == 0){
				return atoi(token->operand[0]) * 3;

			}else if(strcmp(operator, "BYTE") == 0){
				return get_byte_size(token->operand[0]);

			}else if(strcmp(operator, "WORD") == 0){
				return 3;
			}else{
				return -1;
			}
		}

		return -1;
	}

	/*
	 * BYTE operand의 크기 리턴
	 */
	int get_byte_size(char* operand){
		int i;
		char* start;

		switch(operand[0]){
			case 'X':
				start = strstr(operand, "'") + 1;
				for(i = 0; i < strlen(start); i++)
					if(start[i] == '\'')
						return i / 2 + i % 2;
				break;
			case 'C':
				start = strstr(operand, "'") + 1;
				for(i = 0; i < strlen(start); i++)
					if(start[i] == '\'')
						return i;
				break;
			default:
				i = strlen(dectohex(atoi(operand[0])));
				return i / 2 + i % 2;
				break;
		}
	}

}
