/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.generator.cli;

import java.io.File;

import lombok.ToString;

import com.beust.jcommander.Parameter;

/**
 * Command line options.
 */
@ToString
public class Options {

  @Parameter(names = { "-c", "--config" }, required = true, validateValueWith = FileValidator.class, description = "Configuration file (e.g. ./config.yaml)")
  public File config;

  @Parameter(names = { "-d", "--dictionary" }, required = false, validateValueWith = FileValidator.class, description = "Dictionary file (e.g. ./Dictionary.json)")
  public File dictionary;

  @Parameter(names = { "-k", "--codelist" }, required = false, validateValueWith = FileValidator.class, description = "CodeList file (e.g. ./CodeList.json)")
  public File codeList;

  @Parameter(names = { "-v", "--version" }, help = true, description = "Show version")
  public boolean version;

  @Parameter(names = { "-h", "--help" }, help = true, description = "Show help information")
  public boolean help;

}