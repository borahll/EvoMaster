using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Xml.Linq;
using EvoMaster.Client.Util.Extensions;
using EvoMaster.Instrumentation_Shared;
using Mono.Cecil;
using Mono.Cecil.Cil;
using Mono.Cecil.Rocks;
using TypeReference = System.Reflection.Metadata.TypeReference;

namespace EvoMaster.Instrumentation {
    /// <summary>
    /// This class is responsible for instrumenting c# libraries.
    /// Instrumentation is done by the aid of Mono.Cecil library
    /// It generates a new dll file for the instrumented SUT
    /// </summary>
    public class Instrumentator {
        private MethodReference _completedProbe;
        private MethodReference _enteringProbe;
        private MethodReference _enteringBranchProbe;

        private MethodReference _compareAndComputeDistanceProbeForInt;
        private MethodReference _compareAndComputeDistanceProbeForDouble;
        private MethodReference _compareAndComputeDistanceProbeForFloat;
        private MethodReference _compareAndComputeDistanceProbeForLong;
        private MethodReference _compareAndComputeDistanceProbeForShort;

        private MethodReference _computeDistanceForOneArgJumpsProbeForInt;
        private MethodReference _computeDistanceForOneArgJumpsProbeForDouble;
        private MethodReference _computeDistanceForOneArgJumpsProbeForFloat;
        private MethodReference _computeDistanceForOneArgJumpsProbeForLong;
        private MethodReference _computeDistanceForOneArgJumpsProbeForShort;
        private readonly RegisteredTargets _registeredTargets = new RegisteredTargets();

        private readonly List<CodeCoordinate> _alreadyCompletedPoints = new List<CodeCoordinate>();

        /// <summary>
        /// This method instruments an assembly file and saves its instrumented version in the specified destination directory
        /// </summary>
        /// <param name="assembly">Name of the file to be instrumented</param>
        /// <param name="destination">Directory where the instrumented file should be copied in</param>
        /// <exception cref="ArgumentNullException"></exception>
        public void Instrument(string assembly, string destination) {
            if (string.IsNullOrEmpty(assembly)) throw new ArgumentNullException(assembly);
            if (string.IsNullOrEmpty(destination)) throw new ArgumentNullException(destination);

            //ReadSymbols is set true to enable getting line info
            var module =
                ModuleDefinition.ReadModule(assembly, new ReaderParameters {ReadSymbols = true});

            //TODO: check file extension

            _completedProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompletedStatement),
                        new[] {typeof(string), typeof(int), typeof(int)}));

            _enteringProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.EnteringStatement),
                        new[] {typeof(string), typeof(int), typeof(int)}));

            _enteringBranchProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.EnteringBranch),
                        new[] {typeof(string), typeof(int), typeof(int)}));

            _compareAndComputeDistanceProbeForInt =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(int), typeof(int), typeof(string), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForDouble =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(double), typeof(double), typeof(string), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForFloat =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(float), typeof(float), typeof(string), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForLong =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(long), typeof(long), typeof(string), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForShort =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(short), typeof(short), typeof(string), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));

            _computeDistanceForOneArgJumpsProbeForInt = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(int), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForDouble = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(double), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForFloat = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(float), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForLong = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(long), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForShort = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(short), typeof(string), typeof(string), typeof(int), typeof(int)}));

            foreach (var type in module.Types.Where(type => type.Name != "<Module>")) {
                _alreadyCompletedPoints.Clear();

                foreach (var method in type.Methods) {
                    if (!method.HasBody) continue;

                    var ilProcessor = method.Body.GetILProcessor();

                    var mapping = method.DebugInformation.GetSequencePointMapping();

                    var lastEnteredLine = 0;
                    var lastEnteredColumn = 0;

                    method.Body.SimplifyMacros(); //This is to prevent overflow of short branch opcodes

                    var lastBranch = 0; //jumps per line counter

                    var localVariableTypes = new Dictionary<string, string>();

                    for (var i = 0; i < int.MaxValue; i++) {
                        Instruction instruction;
                        try {
                            instruction = method.Body.Instructions[i];
                        }
                        catch (ArgumentOutOfRangeException) {
                            break;
                        }

                        if (instruction.IsStoreLocalVariable()) {
                            switch (instruction.Operand) {
                                case VariableDefinition definition:
                                    localVariableTypes.TryAddOrUpdate(definition.ToString(),
                                        definition.VariableType.Name);
                                    break;
                                case FieldDefinition fieldDefinition:
                                    localVariableTypes.TryAddOrUpdate(fieldDefinition.ToString(),
                                        fieldDefinition.FieldType.Name);
                                    break;
                            }
                        }

                        if (instruction.Next != null && instruction.Next.Next != null) {
                            if (instruction.Next.Next.IsConditionalInstructionWithTwoArgs()) {
                                mapping.TryGetValue(instruction, out var sp);

                                var l = lastEnteredLine;

                                if (sp != null) {
                                    l = sp.StartLine;
                                }

                                if (l != lastEnteredLine) lastBranch = 0;

                                i = InsertEnteringBranchProbe(instruction, method.Body.Instructions, ilProcessor,
                                    method.Parameters.ToList(), localVariableTypes, i,
                                    type.Name, l,
                                    lastBranch);

                                lastBranch++;
                            }
                        }

                        //check if it is brtrue or brfalse, provided that it is not added by instruction replacement
                        if (instruction.IsConditionalJumpWithOneArg() &&
                            !(instruction.Previous.OpCode == OpCodes.Call &&
                              instruction.Previous.ToString().Contains(nameof(Probes.CompareAndComputeDistance)))) {
                            mapping.TryGetValue(instruction, out var sp);

                            var l = lastEnteredLine;

                            if (sp != null) {
                                l = sp.StartLine;
                            }

                            i = InsertComputeDistanceForOneArgJumpsProbe(instruction, ilProcessor,
                                method.Parameters.ToList(), localVariableTypes, i, type.Name, l,
                                lastBranch - 1); //TODO: do further check for branchId
                        }

                        mapping.TryGetValue(instruction, out var sequencePoint);

                        // skip non-return instructions which do not have any sequence point info
                        if ((sequencePoint == null || sequencePoint.IsHidden) && instruction.OpCode != OpCodes.Ret)
                            continue;

                        if (lastEnteredLine != 0 && lastEnteredColumn != 0) {
                            i = InsertCompletedStatementProbe(instruction.Previous, ilProcessor, i, type.Name,
                                lastEnteredLine,
                                lastEnteredColumn);
                        }

                        if (sequencePoint != null) {
                            i = InsertEnteringStatementProbe(instruction, method.Body, ilProcessor,
                                i, type.Name, sequencePoint.StartLine, sequencePoint.StartColumn);

                            lastEnteredColumn = sequencePoint.StartColumn;
                            lastEnteredLine = sequencePoint.StartLine;

                            if (method.Body.Instructions.Last().Equals(instruction) &&
                                instruction.OpCode == OpCodes.Ret) {
                                i = InsertCompletedStatementProbe(instruction, ilProcessor, i, type.Name,
                                    lastEnteredLine, lastEnteredColumn);
                            }
                        }
                    }

                    method.Body.OptimizeMacros(); //Change back Br opcodes to Br.s if possible
                }
            }

            if (destination.Length > 1 && destination[^1] == '/') {
                destination = destination.Remove(destination.Length - 1, 1);
            }

            //saving unitsInfoDto in a json file as we need to retrieve them later during runtime of the instrumented SUT
            var json = Newtonsoft.Json.JsonConvert.SerializeObject(_registeredTargets);
            File.WriteAllText("Targets.json", json);

            module.Write($"{destination}/{assembly}");
            Client.Util.SimpleLogger.Info($"Instrumented File Saved at \"{destination}\"");
        }

        private int InsertCompletedStatementProbe(Instruction instruction,
            ILProcessor ilProcessor, int byteCodeIndex, string className, int lineNo, int columnNo) {
            if (_alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            if (instruction.IsUnConditionalJumpOrExitInstruction()) {
                return InsertCompletedStatementProbe(instruction.Previous, ilProcessor, byteCodeIndex,
                    className, lineNo, columnNo);
            }

            if (instruction.OpCode == OpCodes.Call &&
                instruction.Operand.ToString()!.Contains("EvoMaster.Instrumentation.Probes::ComputingBranchDistance")) {
                return InsertCompletedStatementProbe(instruction.Previous.Previous.Previous.Previous, ilProcessor,
                    byteCodeIndex,
                    className, lineNo, columnNo);
            }

            //TODO: check if we should register statements or not
            //register all targets(description of all targets, including units, lines and branches)
            _registeredTargets.Classes.Add(ObjectiveNaming.ClassObjectiveName(className));
            _registeredTargets.Lines.Add(ObjectiveNaming.LineObjectiveName(className, lineNo));

            _alreadyCompletedPoints.Add(new CodeCoordinate(lineNo, columnNo));

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, columnNo);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _completedProbe);

            ilProcessor.InsertAfter(instruction, methodCallInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, columnNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, classNameInstruction);
            byteCodeIndex++;

            return byteCodeIndex;
        }

        private int InsertEnteringStatementProbe(Instruction instruction, MethodBody methodBody,
            ILProcessor ilProcessor, int byteCodeIndex, string className, int lineNo, int columnNo) {
            //Do not add inserted probe if the statement is already covered by completed probe
            if (_alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            if (instruction.Previous != null && instruction.OpCode == instruction.Previous.OpCode &&
                instruction.OpCode == OpCodes.Leave) {
                return byteCodeIndex; //todo: shouldn't be skipped
            }

            //Check if the current instruction is the first instruction after a finally block
            var updateHandlerEnd = false;
            ExceptionHandler exceptionHandler = null;
            if (instruction.Previous != null && instruction.Previous.OpCode == OpCodes.Endfinally) {
                exceptionHandler = methodBody.ExceptionHandlers.FirstOrDefault(x => x.HandlerEnd == instruction);

                if (exceptionHandler != null) updateHandlerEnd = true;
            }

            if (!updateHandlerEnd && instruction.Previous != null &&
                instruction.Previous.OpCode == OpCodes.Leave) {
                exceptionHandler = methodBody.ExceptionHandlers.FirstOrDefault(x => x.TryEnd == instruction);
                if (exceptionHandler != null)
                    return byteCodeIndex; //todo: shouldn't be skipped
            }

            //prevents the probe becoming unreachable at the end of a try block
            if (instruction.Previous != null && instruction.Previous.OpCode == OpCodes.Leave &&
                instruction.Next.OpCode == OpCodes.Leave) {
                return InsertEnteringStatementProbe(instruction.Next, methodBody, ilProcessor, byteCodeIndex, className,
                    lineNo, columnNo);
            }

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, columnNo);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _enteringProbe);


            ilProcessor.InsertBefore(instruction, classNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, columnNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, methodCallInstruction);
            byteCodeIndex++;

            //update the end of finally block to prevent insertion of the probe at the end of finally block which makes it unreachable
            if (exceptionHandler != null)
                exceptionHandler.HandlerEnd = classNameInstruction;

            instruction.UpdateJumpsToTheCurrentInstruction(classNameInstruction, methodBody.Instructions);

            return byteCodeIndex;
        }

        private int InsertEnteringBranchProbe(Instruction instruction, IEnumerable<Instruction> instructions,
            ILProcessor ilProcessor, IReadOnlyList<ParameterDefinition> methodParams,
            IReadOnlyDictionary<string, string> localVarTypes, int byteCodeIndex,
            string className, int lineNo, int branchId) {
            _registeredTargets.Branches.Add(ObjectiveNaming.BranchObjectiveName(className, lineNo, branchId, true));
            _registeredTargets.Branches.Add(ObjectiveNaming.BranchObjectiveName(className, lineNo, branchId, false));

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var branchIdInstruction = ilProcessor.Create(OpCodes.Ldc_I4, branchId);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _enteringBranchProbe);

            ilProcessor.InsertBefore(instruction, classNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, branchIdInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, methodCallInstruction);
            byteCodeIndex++;

            instruction.UpdateJumpsToTheCurrentInstruction(classNameInstruction, instructions);

            var branchIns = instruction.Next.Next;

            byteCodeIndex =
                InsertCompareAndComputeDistanceProbe(branchIns, ilProcessor, methodParams, localVarTypes, byteCodeIndex,
                    className,
                    lineNo,
                    branchId);

            return byteCodeIndex;
        }

        private int InsertCompareAndComputeDistanceProbe(Instruction branchInstruction, ILProcessor ilProcessor,
            IReadOnlyList<ParameterDefinition> methodParams, IReadOnlyDictionary<string, string> localVarTypes,
            int byteCodeIndex, string className, int lineNo, int branchId) {
            MethodReference probe = null;

            if (branchInstruction.IsConditionalInstructionWithTwoArgs()) {
                var type = branchInstruction.Previous.DetectType(methodParams, localVarTypes) ??
                           branchInstruction.Previous.Previous.DetectType(methodParams, localVarTypes);

                if (type == typeof(int))
                    probe = _compareAndComputeDistanceProbeForInt;
                else if (type == typeof(double))
                    probe = _compareAndComputeDistanceProbeForDouble;
                else if (type == typeof(float))
                    probe = _compareAndComputeDistanceProbeForFloat;
                else if (type == typeof(long))
                    probe = _compareAndComputeDistanceProbeForLong;
                else if (type == typeof(short))
                    probe = _compareAndComputeDistanceProbeForShort;
                //TODO: other types
            }

            if (branchInstruction.IsCompareWithTwoArgs()) {
                byteCodeIndex =
                    InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex, className,
                        lineNo,
                        branchId,
                        branchInstruction.OpCode);

                ilProcessor.Replace(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
            }
            //the rest are the jump instructions which we should replace them with a compare and jump
            //bgt equals to cgt + brtrue
            else if (branchInstruction.OpCode == OpCodes.Bgt || branchInstruction.OpCode == OpCodes.Bgt_Un) {
                byteCodeIndex = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Cgt);
                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brtrue, (Instruction) target));
            }
            //beq equals to ceq + brtrue
            else if (branchInstruction.OpCode == OpCodes.Beq) {
                byteCodeIndex = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Ceq);
                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brtrue, (Instruction) target));
            }
            //bge equals to clt + brfalse
            else if (branchInstruction.OpCode == OpCodes.Bge || branchInstruction.OpCode == OpCodes.Bge_Un) {
                byteCodeIndex = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Clt);
                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brfalse, (Instruction) target));
            }
            //ble equals to cgt + brfalse
            else if (branchInstruction.OpCode == OpCodes.Ble || branchInstruction.OpCode == OpCodes.Ble_Un) {
                byteCodeIndex = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Cgt);
                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brfalse, (Instruction) target));
            }
            //blt equals to clt + brtrue
            else if (branchInstruction.OpCode == OpCodes.Blt || branchInstruction.OpCode == OpCodes.Blt_Un) {
                byteCodeIndex = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Clt);
                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brtrue, (Instruction) target));
            }
            //bne equals to ceq + brfalse
            else if (branchInstruction.OpCode == OpCodes.Bne_Un) {
                byteCodeIndex = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Ceq);
                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brfalse, (Instruction) target));
            }

            return byteCodeIndex;
        }

        private int InsertValuesBeforeBranchInstruction(Instruction instruction, ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int branchId, OpCode originalOpCode,
            OpCode? newOpcode = null) {
            newOpcode ??= originalOpCode;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, originalOpCode.ToString()));
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, newOpcode.Value.ToString()));
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            return byteCodeIndex;
        }

        private int InsertComputeDistanceForOneArgJumpsProbe(Instruction branchInstruction, ILProcessor ilProcessor,
            IReadOnlyList<ParameterDefinition> methodParams, IReadOnlyDictionary<string, string> localVarTypes,
            int byteCodeIndex, string className, int lineNo, int branchId) {
            MethodReference probe = null;
            var type = branchInstruction.Previous.DetectType(methodParams, localVarTypes);

            if (type == typeof(int))
                probe = _computeDistanceForOneArgJumpsProbeForInt;
            else if (type == typeof(double))
                probe = _computeDistanceForOneArgJumpsProbeForDouble;
            else if (type == typeof(float))
                probe = _computeDistanceForOneArgJumpsProbeForFloat;
            else if (type == typeof(long))
                probe = _computeDistanceForOneArgJumpsProbeForLong;
            else if (type == typeof(short))
                probe = _computeDistanceForOneArgJumpsProbeForShort;
            // else probe = _computeDistanceForOneArgJumpsProbeForInt;
            //TODO: other types

            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Dup));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction,
                ilProcessor.Create(OpCodes.Ldstr, branchInstruction.OpCode.ToString()));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            ilProcessor.InsertBefore(branchInstruction,
                ilProcessor.Create(OpCodes.Call, probe));
            byteCodeIndex++;

            return byteCodeIndex;
        }
    }
}